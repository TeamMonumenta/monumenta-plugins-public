package com.playmonumenta.plugins.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.playmonumenta.plugins.attributes.AttributeManager;
import com.playmonumenta.plugins.enchantments.CustomEnchantment;
import com.playmonumenta.plugins.enchantments.Locked;
import com.playmonumenta.plugins.enchantments.StatTrack;
import com.playmonumenta.plugins.utils.InfusionUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.scriptedquests.trades.TradeWindowOpenEvent;

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

	// These lore lines are not considered for lore equality, and will also not be copied over to the final item
	private static final ImmutableSet<String> IGNORED_LORE = ImmutableSet.of("Prismarine Enabled", "Prismarine Disabled", "Blackstone Enabled", "Blackstone Disabled");

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
				// note: we use getLore() in this class instead of lore() to get a unique representation of the lore which is important for equals and subset checks among others
				List<String> sourceLore = source.getLore();
				List<String> resultLore = result.getLore();
				if (sourceLore == null || resultLore == null) {
					continue;
				}
				if (ItemUtils.getItemRegion(source) == ItemUtils.ItemRegion.UNKNOWN
					|| ItemUtils.getItemRegion(source) != ItemUtils.getItemRegion(result)
					|| InfusionUtils.getCostMultiplier(source) <= 0
					|| InfusionUtils.getCostMultiplier(source) != InfusionUtils.getCostMultiplier(result)
					|| !haveSameStats(source, result)) {
					continue;
				}

				// Items for which we made trades already (for the current original trade).
				// Used to not create duplicate trades if the player for some reason has multiple identical items.
				List<ItemStack> createdTrades = new ArrayList<>();

				// Current trade is a re-skin trade, check if the player has matching source items and add new trades if so
				for (ItemStack playerItem : player.getInventory().getStorageContents()) {
					// Skip over empty slots, and skip over items that already match an existing trade exactly
					if (playerItem == null
						|| playerItem.isSimilar(source)
						|| createdTrades.stream().anyMatch(t -> t.isSimilar(playerItem))) {
						continue;
					}
					List<String> playerItemLore = playerItem.getLore();
					// Check that the playerItem has the same base item as the trade's source:
					// - same type (and for shulker boxes, ignore color)
					// - same name
					// - same vanilla enchantments
					// - a superset of lore lines, i.e. all base item lore lines plus optionally some more.
					//   This automatically includes a custom enchantments + attributes check
					//   (ignoring order, but this should not be an issue as the type + name check make stat checks superfluous)
					// The enchantment and lore checks are just a failsafe, as type + name should be enough to specify a tiered item in general.
					if (playerItemLore == null
							|| !(source.getType() == playerItem.getType() || ItemUtils.isShulkerBox(source.getType()) && ItemUtils.isShulkerBox(playerItem.getType()))
							|| !Objects.equals(source.getItemMeta().displayName(), playerItem.getItemMeta().displayName())
							|| !Objects.equals(source.getEnchantments(), playerItem.getEnchantments())
							|| !removeIgnoredLoreLines(playerItemLore).containsAll(removeIgnoredLoreLines(sourceLore))) {
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

					// Modify the result item to carry over extra lore (infusions etc.)
					// Positions are chosen the same way ItemUtils.enchantifyItem does it - infusions after normal enchantments, and anything else before the end or first empty line.
					int enchantsPos = resultLore.size();
					int othersPos = resultLore.size();
					for (int j = 0; j < resultLore.size(); j++) {
						String loreStripped = ChatColor.stripColor(resultLore.get(j)).trim();
						if (enchantsPos == resultLore.size()
							&& (loreStripped.contains("King's Valley :") ||
							loreStripped.contains("Celsian Isles :") ||
							loreStripped.contains("Monumenta :") ||
							loreStripped.contains("Armor") ||
							loreStripped.contains("Magic Wand") ||
							loreStripped.contains("Alchemical Utensil"))) {
							enchantsPos = j;
						} else if (loreStripped.isEmpty()) {
							if (enchantsPos == resultLore.size()) {
								enchantsPos = j;
							}
							othersPos = j;
							break;
						}
					}
					List<String> extraLore = new ArrayList<>(playerItemLore);
					extraLore.removeAll(sourceLore);
					extraLore = removeIgnoredLoreLines(extraLore);
					// Kaul reskins add Hope (and unskinning removes it), so need to handle these specially to prevent double hope or removing hope
					if (CustomEnchantment.HOPE.getEnchantment().getItemLevel(source) == 0
						&& CustomEnchantment.HOPE.getEnchantment().getItemLevel(result) > 0
						&& CustomEnchantment.HOPE.getEnchantment().getItemLevel(playerItem) > 0) {
						// reskin adds Hope and player item already has hope: remove the extra Hope line
						extraLore.remove(CustomEnchantment.HOPE.getEnchantment().getProperty());
					} else if (CustomEnchantment.HOPE.getEnchantment().getItemLevel(source) > 0
						&& CustomEnchantment.HOPE.getEnchantment().getItemLevel(result) == 0
						&& extraLore.stream().anyMatch(s -> ChatColor.stripColor(s).startsWith("Infused by "))) {
						// reskin removes Hope and player item is manually hoped: add the missing Hope line
						extraLore.add(0, CustomEnchantment.HOPE.getEnchantment().getProperty());
					}
					List<String> newLore = new ArrayList<>(resultLore);
					for (String extra : extraLore) {
						int pos = isCustomEnchantmentLoreLine(extra) ? enchantsPos : othersPos;
						newLore.add(pos, extra);
						if (pos <= enchantsPos) {
							enchantsPos++;
						}
						if (pos <= othersPos) {
							othersPos++;
						}
					}
					newResult.setLore(newLore);
					ItemUtils.setPlainLore(newResult);

					// Carry over the durability to not make the trade repair items (a possible shattered state is copied via lore)
					if (newResult.getItemMeta() instanceof Damageable && playerItem.getItemMeta() instanceof Damageable) {
						Damageable newResultMeta = (Damageable) newResult.getItemMeta();
						newResultMeta.setDamage(((Damageable) playerItem.getItemMeta()).getDamage());
						newResult.setItemMeta((ItemMeta) newResultMeta);
					}

					// Carry over the current arrow of a crossbow if the player item has an arrow but the result item doesn't have one
					if (newResult.getItemMeta() instanceof CrossbowMeta && playerItem.getItemMeta() instanceof CrossbowMeta
						&& !((CrossbowMeta) newResult.getItemMeta()).hasChargedProjectiles() && ((CrossbowMeta) playerItem.getItemMeta()).hasChargedProjectiles()) {
						CrossbowMeta newResultMeta = (CrossbowMeta) newResult.getItemMeta();
						newResultMeta.setChargedProjectiles(((CrossbowMeta) playerItem.getItemMeta()).getChargedProjectiles());
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
		for (CustomEnchantment ench : CustomEnchantment.values()) {
			// Hope and Hopeless are treated as equivalent for re-skin purposes. They are skipped here and checked below.
			// Divine Aura is a bonus enchantment for Kaul reskins
			if (ench == CustomEnchantment.HOPE || ench == CustomEnchantment.HOPELESS
				|| ench == CustomEnchantment.DIVINE_AURA) {
				continue;
			}
			if (ench.getEnchantment().getItemLevel(i1) != ench.getEnchantment().getItemLevel(i2)) {
				return false;
			}
		}
		// Check Hope/Hopeless
		// If either item has Divine Aura, skip the Hope check completely, as Hope is added as a bonus during such trades
		if (CustomEnchantment.DIVINE_AURA.getEnchantment().getItemLevel(i1) == 0 && CustomEnchantment.DIVINE_AURA.getEnchantment().getItemLevel(i2) == 0
			&& CustomEnchantment.HOPE.getEnchantment().getItemLevel(i1) + CustomEnchantment.HOPELESS.getEnchantment().getItemLevel(i1)
			!= CustomEnchantment.HOPE.getEnchantment().getItemLevel(i2) + CustomEnchantment.HOPELESS.getEnchantment().getItemLevel(i2)) {
			return false;
		}

		// vanilla attributes
		Multimap<Attribute, AttributeModifier> vanillaMods1 = i1.getItemMeta().getAttributeModifiers();
		Multimap<Attribute, AttributeModifier> vanillaMods2 = i2.getItemMeta().getAttributeModifiers();
		if (vanillaMods1 != null && vanillaMods2 != null) {
			for (Attribute attr : Attribute.values()) {
				// We need to filter out modifiers that have no effect - these sometimes exist and mess up the comparison
				Collection<AttributeModifier> mods1 = vanillaMods1.get(attr).stream().filter(mod -> mod.getAmount() != 0).collect(Collectors.toList());
				Collection<AttributeModifier> mods2 = vanillaMods2.get(attr).stream().filter(mod -> mod.getAmount() != 0).collect(Collectors.toList());
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

		// custom attributes
		// directly compared via lore lines, so the attributes must be in exactly the same order for this to work (and they really should be)
		Predicate<String> isLoreAttribLine = lore ->
			Arrays.stream(com.playmonumenta.plugins.itemindex.Attribute.values()).anyMatch(attr -> attr.isCustom() && lore.startsWith(attr.getReadableStringFormat()))
				|| Arrays.stream(AttributeManager.ATTRIBUTE_INDICATORS).skip(1).anyMatch(indicator -> lore.equals(ChatColor.stripColor(indicator)));
		List<String> loreAttribs1 = i1.getLore() != null ? i1.getLore().stream().map(l -> ChatColor.stripColor(l).trim()).filter(isLoreAttribLine).collect(Collectors.toList()) : null;
		List<String> loreAttribs2 = i1.getLore() != null ? i2.getLore().stream().map(l -> ChatColor.stripColor(l).trim()).filter(isLoreAttribLine).collect(Collectors.toList()) : null;
		if (!Objects.equals(loreAttribs1, loreAttribs2)) {
			return false;
		}

		// if we get here, the items have the same stats
		return true;
	}

	private static List<String> removeIgnoredLoreLines(List<String> lore) {
		List<String> reducedLore = new ArrayList<>(lore);
		reducedLore.removeIf(c -> IGNORED_LORE.contains(ChatColor.stripColor(c)));
		return reducedLore;
	}

	private static boolean isCustomEnchantmentLoreLine(String lore) {
		String plainLore = ChatColor.stripColor(lore);

		for (CustomEnchantment ench : CustomEnchantment.values()) {
			// "Fake" enchantments for Celsian Isles items. These match "Celsian Isles : " so would consider the region tag an enchantment
			if (ench == CustomEnchantment.REGION_SCALING_DAMAGE_DEALT || ench == CustomEnchantment.REGION_SCALING_DAMAGE_TAKEN) {
				continue;
			}
			String name = ChatColor.stripColor(ench.getEnchantment().getProperty());
			if (plainLore.startsWith(name)
				&& !plainLore.startsWith(name + " by")) { // "Gilded by", and maybe future ones
				return true;
			}
		}

		// Locked is not in the CustomEnchantment enum, so an extra check is needed
		if (plainLore.startsWith(ChatColor.stripColor(Locked.PROPERTY_NAME))) {
			return true;
		}

		for (StatTrack.StatTrackOptions ench : StatTrack.StatTrackOptions.values()) {
			if (plainLore.startsWith(ench.getEnchantName())) {
				return true;
			}
		}

		return false;
	}

}
