package com.playmonumenta.plugins.listeners;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.playmonumenta.plugins.utils.InfusionUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.scriptedquests.trades.TradeWindowOpenEvent;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTCompoundList;
import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Listens to ScriptedQuest's TradeWindowOpenEvent to dynamically change shown trades.
 * Currently, this entails adding new trades for "re-skin" trades that match existing items in a player's inventory to allow re-skinning of items with infusions and other modifiers.
 */
public class TradeListener implements Listener {

	// Ignore stat checks for trades between items in these sets
	private static final ImmutableSet<ImmutableSet<String>> SKIP_STAT_CHECK_TRADES = ImmutableSet.of(
		ImmutableSet.of("King's Warden", "Queen's Warden", "Kaul's Warden"),
		ImmutableSet.of("Frost Giant's Greatsword", "Frost Giant's Crusher", "Frost Giant's Staff", "Frost Giant's Crescent"));

	// Items for which to completely disable reskin trades
	private static final ImmutableSet<String> DISABLED_ITEMS = ImmutableSet.of("Soulsinger");

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void tradeWindowOpenEvent(TradeWindowOpenEvent event) {

		// For "re-skin" trades, add trades matching a player's existing items that preserve added infusions etc.

		Player player = event.getPlayer();
		List<TradeWindowOpenEvent.Trade> trades = event.getTrades();
		int numTrades = trades.size();
		for (int i = 0; i < numTrades; i++) {
			TradeWindowOpenEvent.Trade trade = trades.get(i);
			MerchantRecipe recipe = trade.getRecipe();
			for (int slot = 0; slot < recipe.getIngredients().size(); slot++) {

				// First check if the trade is a re-skin trade.
				// To do this we check the following on the source and result items:
				// - stack size is 1
				// - same region (and have a region at all)
				// - tiers are equivalent for infusions (same costs, and are infusable in the first place)
				// - same enchantments (both vanilla and custom)
				// - same attributes (both vanilla and custom)
				// some items, like Kaul's Warden, skip some of these checks, while others, like Soulsinger, have any reskins disabled
				ItemStack source = recipe.getIngredients().get(slot);
				ItemStack result = recipe.getResult();
				if (source.getAmount() != 1 || result.getAmount() != 1) {
					continue;
				}
				if (DISABLED_ITEMS.contains(ItemUtils.getPlainNameIfExists(source))
					    || DISABLED_ITEMS.contains(ItemUtils.getPlainNameIfExists(result))) {
					continue;
				}
				if (ItemStatUtils.getRegion(source) == ItemStatUtils.Region.NONE
					    || ItemStatUtils.getRegion(source) != ItemStatUtils.getRegion(result)
					    || InfusionUtils.getCostMultiplier(source) <= 0
					    || InfusionUtils.getCostMultiplier(source) != InfusionUtils.getCostMultiplier(result)
					    || !haveSameStats(source, result)) {
					continue;
				}

				// Items for which we made trades already (for the current original trade).
				// Used to not create duplicate trades if the player for some reason has multiple identical items.
				List<ItemStack> createdTrades = new ArrayList<>();

				// Current trade is a re-skin trade, check if the player has matching source items and add new trades if so
				for (ItemStack playerItem : player.getInventory().getContents()) {
					// Skip over empty slots, and skip over items that already match an existing trade exactly
					if (playerItem == null
						    || playerItem.isSimilar(source)
						    || createdTrades.stream().anyMatch(t -> t.isSimilar(playerItem))) {
						continue;
					}
					// Check that the playerItem has the same base item as the trade's source:
					// - same type (and for shulker boxes, ignore color)
					// - same plain name (or both have no plain name)
					if (!(source.getType() == playerItem.getType() || ItemUtils.isShulkerBox(source.getType()) && ItemUtils.isShulkerBox(playerItem.getType()))
						    || !Objects.equals(ItemUtils.getPlainNameIfExists(source), ItemUtils.getPlainNameIfExists(playerItem))) {
						continue;
					}

					// Shulkers with contents are janky - the trades work, but the trades without contents work on them as well, clearing any content.
					// Thus we don't allow trades with non-empty Shulkers
					if (playerItem.getItemMeta() instanceof BlockStateMeta
						    && ((BlockStateMeta) playerItem.getItemMeta()).getBlockState() instanceof ShulkerBox
						    && !((ShulkerBox) ((BlockStateMeta) playerItem.getItemMeta()).getBlockState()).getInventory().isEmpty()) {
						continue;
					}

					// We cannot use the default ItemStack.clone() here as that doesn't clone NBT properly, causing some edits of the clone to also change the original item (at least for Shulkers' plain lore)
					ItemStack newResult = ItemUtils.clone(result);

					// Modify the result item to carry over player modifications (infusions etc.)
					NBTItem playerItemNbt = new NBTItem(playerItem);
					NBTItem newResultNbt = new NBTItem(newResult);
					ItemStatUtils.addPlayerModified(newResultNbt).mergeCompound(ItemStatUtils.getPlayerModified(playerItemNbt));
					newResult = newResultNbt.getItem();

					// Kaul skins can add or remove Hope, so need to handle this specially to prevent incorrectly doubling/removing/retaining Hope
					// TODO may need to be changed for item rework
					/*if (CustomEnchantment.HOPE.getEnchantment().getItemLevel(source) == 0
						&& CustomEnchantment.HOPE.getEnchantment().getItemLevel(result) > 0
						&& CustomEnchantment.HOPE.getEnchantment().getItemLevel(playerItem) > 0) {
						// reskin adds Hope and player item already has hope: remove the extra Hope line
						extraLore.remove(CustomEnchantment.HOPE.getEnchantment().getProperty());
					} else if (CustomEnchantment.HOPE.getEnchantment().getItemLevel(source) > 0
						&& CustomEnchantment.HOPE.getEnchantment().getItemLevel(result) == 0
						&& extraLore.stream().anyMatch(s -> ChatColor.stripColor(s).startsWith("Infused by "))) {
						// reskin removes Hope and player item is manually hoped: add the missing Hope line
						extraLore.add(0, CustomEnchantment.HOPE.getEnchantment().getProperty());
					}*/

					ItemStatUtils.generateItemStats(newResult);

					// Carry over the durability to not make the trade repair items (a possible shattered state is copied via lore)
					if (newResult.getItemMeta() instanceof Damageable newResultMeta && playerItem.getItemMeta() instanceof Damageable playerItemMeta) {
						newResultMeta.setDamage(playerItemMeta.getDamage());
						newResult.setItemMeta((ItemMeta) newResultMeta);
					}

					// Carry over the current arrow of a crossbow if the player item has an arrow but the result item doesn't have one
					if (newResult.getItemMeta() instanceof CrossbowMeta newResultMeta && playerItem.getItemMeta() instanceof CrossbowMeta playerItemMeta
						    && !newResultMeta.hasChargedProjectiles() && playerItemMeta.hasChargedProjectiles()) {
						newResultMeta.setChargedProjectiles(playerItemMeta.getChargedProjectiles());
						newResult.setItemMeta(newResultMeta);
					}

					// NB: we do not copy over all item metadata in general, as some items differ enough for that to cause issues
					// e.g. some reskins are from a player head, which has a texture, to an unbreakable helmet, which has no such texture.

					// New source item is the player's item without any changes except the stack size changed to 1
					ItemStack newSource = ItemUtils.clone(playerItem);
					newSource.setAmount(1);

					// Finally, create the new trade
					MerchantRecipe newRecipe = new MerchantRecipe(newResult, recipe.getUses(), recipe.getMaxUses(), recipe.hasExperienceReward(),
					                                              recipe.getVillagerExperience(), recipe.getPriceMultiplier(), recipe.shouldIgnoreDiscounts());
					List<ItemStack> newIngredients = new ArrayList<>(recipe.getIngredients());
					newIngredients.set(slot, newSource);
					newRecipe.setIngredients(newIngredients);
					trades.add(new TradeWindowOpenEvent.Trade(newRecipe, trade.getActions()));

					createdTrades.add(playerItem);
				}
			}
		}
	}

	/**
	 * Checks if two items have the same stats (enchantments, attributes) as far as is relevant for re-skin trades
	 */
	private static boolean haveSameStats(ItemStack i1, ItemStack i2) {
		NBTItem nbt1 = new NBTItem(i1);
		NBTItem nbt2 = new NBTItem(i2);

		// trades with ignored stat checks
		for (Set<String> enabledtrade : SKIP_STAT_CHECK_TRADES) {
			if (enabledtrade.contains(ItemUtils.getPlainNameIfExists(i1)) && enabledtrade.contains(ItemUtils.getPlainNameIfExists(i2))) {
				return true;
			}
		}

		// vanilla enchantments
		if (!Objects.equals(i1.getEnchantments(), i2.getEnchantments())) {
			return false;
		}


		// custom enchantments
		// cannot compare NBT directly due to Divine Aura
		NBTCompound enchantments1 = ItemStatUtils.getEnchantments(nbt1);
		NBTCompound enchantments2 = ItemStatUtils.getEnchantments(nbt2);
		for (EnchantmentType ench : EnchantmentType.values()) {
			// Divine Aura is a bonus enchantment for Kaul reskins, si ignore it
			if (ench == EnchantmentType.DIVINE_AURA) {
				continue;
			}
			if (ItemStatUtils.getEnchantmentLevel(enchantments1, ench) != ItemStatUtils.getEnchantmentLevel(enchantments2, ench)) {
				return false;
			}
		}
		// Check Hope
		// If either item has Divine Aura, skip the Hope check completely, as Hope is added as a bonus during such trades
		if (ItemStatUtils.getEnchantmentLevel(enchantments1, EnchantmentType.DIVINE_AURA) == 0
			    && ItemStatUtils.getEnchantmentLevel(enchantments2, EnchantmentType.DIVINE_AURA) == 0
			    && ItemStatUtils.getInfusionLevel(enchantments1, InfusionType.HOPE) != ItemStatUtils.getInfusionLevel(enchantments2, InfusionType.HOPE)) {
			return false;
		}

		// vanilla attributes
		Multimap<Attribute, AttributeModifier> vanillaMods1 = i1.getItemMeta().getAttributeModifiers();
		Multimap<Attribute, AttributeModifier> vanillaMods2 = i2.getItemMeta().getAttributeModifiers();
		if (vanillaMods1 != null && vanillaMods2 != null) {
			for (Attribute attr : Attribute.values()) {
				// We need to filter out modifiers that have no effect - these sometimes exist and mess up the comparison
				Collection<AttributeModifier> mods1 = vanillaMods1.get(attr).stream().filter(mod -> mod.getAmount() != 0).toList();
				Collection<AttributeModifier> mods2 = vanillaMods2.get(attr).stream().filter(mod -> mod.getAmount() != 0).toList();
				// to check equality with a custom predicate, check that mods1 and mods2 have the same size,
				// and that every modifier in mods1 is present in mods2 and vice versa
				if (mods1.size() != mods2.size()) {
					return false;
				}
				BiPredicate<AttributeModifier, AttributeModifier> modsMatch = (mod1, mod2) -> mod1.getSlot() == mod2.getSlot() && mod1.getOperation() == mod2.getOperation() && mod1.getAmount() == mod2.getAmount();
				if (mods1.stream().anyMatch(mod1 -> mods2.stream().noneMatch(mod2 -> modsMatch.test(mod1, mod2)))
					    || mods2.stream().anyMatch(mod2 -> mods1.stream().noneMatch(mod1 -> modsMatch.test(mod1, mod2)))) {
					return false;
				}
			}
		} else if (vanillaMods1 != null || vanillaMods2 != null) { // one item has mods but the other doesn't, so they differ
			return false;
		}

		// custom attributes - compare NBT directly, ignoring order
		NBTCompoundList attrs1 = ItemStatUtils.getAttributes(nbt1);
		NBTCompoundList attrs2 = ItemStatUtils.getAttributes(nbt2);
		if ((attrs1 == null) != (attrs2 == null)) {
			// Different nullness - objects are different
			return false;
		} else if (attrs1 != null && attrs2 != null) {
			// Both are non-null, compare contents without order
			return Set.copyOf(attrs1).equals(Set.copyOf(attrs2));
		}

		// if we get here, the items have the same stats
		return true;
	}

}
