package com.playmonumenta.plugins.listeners;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.playmonumenta.plugins.utils.InfusionUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.scriptedquests.trades.TradeWindowOpenEvent;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTCompoundList;
import de.tr7zw.nbtapi.NBTItem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.LeatherArmorMeta;

/**
 * Listens to ScriptedQuest's TradeWindowOpenEvent to dynamically change shown trades.
 * <ul>
 *     <li>Adds new trades for "re-skin" trades that match existing items in a player's inventory to allow re-skinning of items with infusions and other modifiers.
 *     <li>Adds a reduced version of the above for other trades that only keeps infusions whose price is constant (delve infusions, Locked, Hope, etc.)
 *     <li>Add new trades for modified shulker box, leather armor, and shield (dyed, different banner pattern) (except for Arena or Terth trades)
 * </ul>
 * Also prevents doing trades with filled shulker boxes to prevent deleting their contents
 */
public class TradeListener implements Listener {

	// Ignore stat checks for trades between items in these sets
	private static final ImmutableSet<ImmutableSet<String>> SKIP_STAT_CHECK_TRADES = ImmutableSet.of(
		ImmutableSet.of("King's Warden", "Queen's Warden", "Kaul's Warden"),
		ImmutableSet.of("Frost Giant's Greatsword", "Frost Giant's Crusher", "Frost Giant's Staff", "Frost Giant's Crescent"),
		ImmutableSet.of("True North", "Truer North", "Truest North"));

	// Infusions whose cost depends on item tier and/or region. These can only be moved in re-skin trades.
	private static final ItemStatUtils.InfusionType[] VARYING_COST_INFUSIONS = {
		ItemStatUtils.InfusionType.ACUMEN,
		ItemStatUtils.InfusionType.FOCUS,
		ItemStatUtils.InfusionType.PERSPICACITY,
		ItemStatUtils.InfusionType.TENACITY,
		ItemStatUtils.InfusionType.VIGOR,
		ItemStatUtils.InfusionType.VITALITY,

		// not quite a "varying cost infusion", but still prevent this from being moved to other items unless it's a re-skin trade
		ItemStatUtils.InfusionType.SOULBOUND
	};

	// Items for which to completely disable reskin/dye trades
	private static final ImmutableSet<String> DISABLED_ITEMS = ImmutableSet.of("Soulsinger");

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void tradeWindowOpenEvent(TradeWindowOpenEvent event) {

		// For "re-skin" trades, add trades matching a player's existing items that preserve added infusions etc.

		Player player = event.getPlayer();
		List<TradeWindowOpenEvent.Trade> trades = event.getTrades();
		int numTrades = trades.size();
		for (int i = 0; i < numTrades; i++) {
			TradeWindowOpenEvent.Trade trade = trades.get(i);
			if (trade.getOriginalResult() != null) {
				// Only not null if we are modifying the count, which will never happen when we are reskinning
				continue;
			}
			MerchantRecipe recipe = trade.getRecipe();
			handleReskinTrades(player, trades, trade, recipe);
			handleDyedTrades(player, trades, trade, recipe);
		}
	}

	private static void handleReskinTrades(Player player, List<TradeWindowOpenEvent.Trade> trades, TradeWindowOpenEvent.Trade trade, MerchantRecipe recipe) {

		for (int slot = 0; slot < recipe.getIngredients().size(); slot++) {

			// First check if the trade is a re-skin trade.
			// To do this we check the following on the source and result items:
			// - stack size is 1
			// - same region (and have a region at all)
			// - tiers are equivalent for infusions (same costs, and are infusable in the first place)
			// - same enchantments (both vanilla and custom)
			// - same attributes (both vanilla and custom)
			// some items, like Kaul's Warden, skip some of these checks, while others, like Soulsinger, have any reskins disabled
			// if it's not a re-skin trade, continue on, but only consider player items that have no infusions whose cost is region or tier-dependent
			ItemStack source = recipe.getIngredients().get(slot);
			ItemStack result = recipe.getResult();
			if (source.getAmount() != 1 || result.getAmount() != 1) {
				continue;
			}
			if (DISABLED_ITEMS.contains(ItemUtils.getPlainNameIfExists(source))
				    || DISABLED_ITEMS.contains(ItemUtils.getPlainNameIfExists(result))) {
				continue;
			}
			// only consider trades with "Monumenta items" - these should all have a region
			if (ItemStatUtils.getRegion(source) == ItemStatUtils.Region.NONE
				    || ItemStatUtils.getRegion(result) == ItemStatUtils.Region.NONE) {
				return;
			}
			boolean carryOverVaryingCostInfusions = ItemStatUtils.getRegion(source) == ItemStatUtils.getRegion(result)
				                                        && InfusionUtils.getCostMultiplier(source) > 0
				                                        && InfusionUtils.getCostMultiplier(source) == InfusionUtils.getCostMultiplier(result)
				                                        && haveSameStats(source, result);

			// Items for which we made trades already (for the current original trade).
			// Used to not create duplicate trades if the player for some reason has multiple identical items.
			List<ItemStack> createdTrades = new ArrayList<>();

			// Current trade is a re-skin trade, check if the player has matching source items and add new trades if so
			playerItemLoop:
			for (ItemStack playerItem : player.getInventory().getContents()) {
				// Skip over empty slots, and skip over items that already match an existing trade exactly
				if (playerItem == null
					    || playerItem.getType() == Material.AIR
					    || playerItem.isSimilar(source)
					    || createdTrades.stream().anyMatch(t -> t.isSimilar(playerItem))) {
					continue;
				}
				// Check that the playerItem has the same base item as the trade's source:
				// - same type (and for shulker boxes, ignore color)
				// - same plain name (or both have no plain name)
				if (!(source.getType() == playerItem.getType() || (ItemUtils.isShulkerBox(source.getType()) && ItemUtils.isShulkerBox(playerItem.getType())))
					    || !Objects.equals(ItemUtils.getPlainNameIfExists(source), ItemUtils.getPlainNameIfExists(playerItem))) {
					continue;
				}
				// if not a re-skin trade, do not allow moving varying cost infusions
				if (!carryOverVaryingCostInfusions) {
					for (ItemStatUtils.InfusionType varyingCostInfusion : VARYING_COST_INFUSIONS) {
						if (ItemStatUtils.getInfusionLevel(playerItem, varyingCostInfusion) > 0) {
							continue playerItemLoop;
						}
					}
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
				NBTCompound playerModified = ItemStatUtils.getPlayerModified(playerItemNbt);
				if (playerModified == null) { // no modifications, skip this item
					continue;
				}
				ItemStatUtils.addPlayerModified(newResultNbt).mergeCompound(playerModified);
				newResult = newResultNbt.getItem();

				ItemStatUtils.generateItemStats(newResult);

				// Carry over the durability to not make the trade repair items (a possible shattered state is copied via playerModified tag)
				if (newResult.getItemMeta() instanceof Damageable newResultMeta && playerItem.getItemMeta() instanceof Damageable playerItemMeta) {
					newResultMeta.setDamage(playerItemMeta.getDamage());
					newResult.setItemMeta(newResultMeta);
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
				TradeWindowOpenEvent.Trade newTrade = new TradeWindowOpenEvent.Trade(trade);
				newTrade.setRecipe(newRecipe);
				trades.add(newTrade);

				createdTrades.add(playerItem);
			}
		}

	}

	private static void handleDyedTrades(Player player, List<TradeWindowOpenEvent.Trade> trades, TradeWindowOpenEvent.Trade trade, MerchantRecipe recipe) {

		// Items for which we made trades already (for the current original trade).
		// Used to not create duplicate trades if the player for some reason has multiple identical items.
		List<ItemStack> createdTrades = new ArrayList<>();

		for (int slot = 0; slot < recipe.getIngredients().size(); slot++) {
			ItemStack source = recipe.getIngredients().get(slot);
			if (source == null || source.getType() == Material.AIR) {
				continue;
			}

			Predicate<Material> isSameType = type -> type == source.getType();
			Consumer<ItemStack> clearDye;
			BiConsumer<ItemStack, ItemStack> copyDye;
			if (ItemUtils.isShulkerBox(source.getType())) {
				isSameType = ItemUtils::isShulkerBox;
				clearDye = itemStack -> itemStack.setType(Material.SHULKER_BOX);
				copyDye = (from, to) -> to.setType(from.getType());
			} else if (source.getItemMeta() instanceof LeatherArmorMeta
				           && !ItemUtils.getPlainLoreIfExists(source).contains("Arena of Terth")) {
				clearDye = itemStack -> {
					LeatherArmorMeta meta = (LeatherArmorMeta) itemStack.getItemMeta();
					meta.setColor(Color.WHITE);
					itemStack.setItemMeta(meta);
				};
				copyDye = (from, to) -> {
					LeatherArmorMeta fromMeta = (LeatherArmorMeta) from.getItemMeta();
					LeatherArmorMeta toMeta = (LeatherArmorMeta) from.getItemMeta();
					toMeta.setColor(fromMeta.getColor());
					to.setItemMeta(toMeta);
				};
			} else if (source.getType() == Material.SHIELD) {
				// Using Bukkit's shield API doesn't work properly 8and doesn't support shields without banners), so edit NBT directly
				clearDye = itemStack -> {
					new NBTItem(itemStack, true).removeKey("BlockEntityTag");
				};
				copyDye = (from, to) -> {
					NBTItem nbt = new NBTItem(to, true);
					nbt.removeKey("BlockEntityTag");
					NBTCompound fromBanner = new NBTItem(from).getCompound("BlockEntityTag");
					if (fromBanner != null) {
						nbt.getOrCreateCompound("BlockEntityTag").mergeCompound(fromBanner);
					}
				};
			} else {
				continue;
			}

			for (ItemStack playerItem : player.getInventory().getContents()) {
				if (playerItem == null
					    || playerItem.getType() == Material.AIR
					    || !isSameType.test(playerItem.getType())
					    || playerItem.isSimilar(source)
					    || createdTrades.stream().anyMatch(t -> t.isSimilar(playerItem))) {
					continue;
				}

				// check that the items are similar ignoring dye
				ItemStack clearedPlayerItem = ItemUtils.clone(playerItem);
				clearDye.accept(clearedPlayerItem);
				ItemStack clearedSourceItem = ItemUtils.clone(source);
				clearDye.accept(clearedSourceItem);
				if (!clearedPlayerItem.isSimilar(clearedSourceItem)) {
					continue;
				}

				ItemStack newSource = ItemUtils.clone(source);
				copyDye.accept(playerItem, newSource);

				ItemStack originalResult = trade.getOriginalResult();
				if (originalResult != null) {
					copyDye.accept(playerItem, originalResult);
				}

				// create the new trade
				MerchantRecipe newRecipe = new MerchantRecipe(recipe.getResult(), recipe.getUses(), recipe.getMaxUses(), recipe.hasExperienceReward(),
					recipe.getVillagerExperience(), recipe.getPriceMultiplier(), recipe.shouldIgnoreDiscounts());
				List<ItemStack> newIngredients = new ArrayList<>(recipe.getIngredients());
				newIngredients.set(slot, newSource);
				newRecipe.setIngredients(newIngredients);
				TradeWindowOpenEvent.Trade newTrade = new TradeWindowOpenEvent.Trade(trade);
				newTrade.setRecipe(newRecipe);
				trades.add(newTrade);

				createdTrades.add(playerItem);
			}
		}
	}

	/**
	 * Checks if two items have the same stats (enchantments, attributes) as far as is relevant for re-skin trades
	 */
	private static boolean haveSameStats(ItemStack i1, ItemStack i2) {
		NBTItem nbt1 = new NBTItem(i1);
		NBTItem nbt2 = new NBTItem(i2);

		// alchemist bags trades
		if (ItemUtils.isAlchemistItem(i1) && ItemUtils.isAlchemistItem(i2)) {
			return true;
		}

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
			// Divine Aura is a bonus enchantment for Kaul reskins, so ignore it
			if (ench == EnchantmentType.DIVINE_AURA || ench == EnchantmentType.UNBREAKABLE) {
				continue;
			}
			if (ItemStatUtils.getEnchantmentLevel(enchantments1, ench) != ItemStatUtils.getEnchantmentLevel(enchantments2, ench)) {
				return false;
			}
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

	// prevent trading with non-empty shulker boxes
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (event.getClickedInventory() instanceof MerchantInventory merchantInventory
			    && event.getSlot() == 2 // result slot
			    && (isShulkerBoxWithContents(merchantInventory, 0) || isShulkerBoxWithContents(merchantInventory, 1))) {
			event.setCancelled(true);
		}
	}

	private static boolean isShulkerBoxWithContents(MerchantInventory merchantInventory, int slot) {
		if (slot >= merchantInventory.getSize()) {
			return false;
		}
		ItemStack item = merchantInventory.getItem(slot);
		if (item == null || !ItemUtils.isShulkerBox(item.getType())) {
			return false;
		}
		if (!(item.getItemMeta() instanceof BlockStateMeta meta)
			    || !(meta.getBlockState() instanceof ShulkerBox shulkerBox)
			    || shulkerBox.getInventory().isEmpty()) {
			return false;
		}
		// After this point, a shulker box with contents is in the trade.
		// Only allow the trade if the trade requires a shulker box with the exact same contents.
		MerchantRecipe merchantRecipe = merchantInventory.getSelectedRecipe();
		if (merchantRecipe == null) {
			return true;
		}
		List<ItemStack> ingredients = merchantRecipe.getIngredients();
		if (slot >= ingredients.size()) {
			return true;
		}
		return !item.isSimilar(ingredients.get(slot));
	}

}
