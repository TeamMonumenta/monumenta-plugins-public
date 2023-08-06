package com.playmonumenta.plugins.inventories;

import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.itemupdater.ItemUpdateHelper;
import com.playmonumenta.plugins.listeners.QuiverListener;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.scriptedquests.trades.TradeWindowOpenEvent;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBTCompoundList;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import de.tr7zw.nbtapi.iface.ReadableNBTList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

public class CustomContainerItemManager implements Listener {

	/**
	 * Key in Monumenta.PlayerModified that stores how many of that item are actually in the container.
	 * Tag type is Long.
	 */
	public static final String AMOUNT_KEY = "AmountInContainer";

	public static final String SHOW_AMOUNTS_TAG = "ShowContainerAmounts";

	public static final String SHOW_AMOUNTS_AS_STACKS_TAG = "ShowContainerAmountsAsStacks";

	public abstract static class CustomContainerItemConfiguration {

		// 0 for these fields means no limit
		public int mTypesLimit;
		public int mTotalItemsLimit;
		public int mItemsPerTypeLimit;

		public CustomContainerItemConfiguration(ItemStack container) {
			mTypesLimit = ItemStatUtils.getCustomInventoryItemTypesLimit(container);
			mTotalItemsLimit = ItemStatUtils.getCustomInventoryTotalItemsLimit(container);
			mItemsPerTypeLimit = ItemStatUtils.getCustomInventoryItemsPerTypeLimit(container);
		}

		public CustomContainerItemConfiguration() {
		}

		public abstract boolean canPutIntoContainer(ItemStack item);

		public boolean canQuickDeposit() {
			return true;
		}

		public boolean soulBoundOnTrade() {
			return false;
		}

		public void createAdditionalGuiItems(ItemStack container, Gui gui) {
		}

		public boolean checkCanUse(Player player) {
			return true;
		}

		public void generateDescription(ReadableNBT monumenta, Consumer<Component> addLore) {
			ReadableNBTList<ReadWriteNBT> items = monumenta.getCompound(ItemStatUtils.PLAYER_MODIFIED_KEY)
				                        .getCompoundList(ItemStatUtils.ITEMS_KEY);
			long amount = 0;
			for (ReadWriteNBT compound : items) {
				ReadWriteNBT playerModified = ItemStatUtils.getPlayerModified(compound.getOrCreateCompound("tag"));
				if (playerModified == null) {
					continue;
				}
				long tempAmount = playerModified.getLong(CustomContainerItemManager.AMOUNT_KEY);
				amount += tempAmount;
			}
			addLore.accept(Component.text("Contains ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
				               .append(Component.text(amount, NamedTextColor.WHITE))
				               .append(Component.text(" items", NamedTextColor.GRAY)));
		}
	}

	public static @Nullable CustomContainerItemConfiguration getConfiguration(ItemStack item) {
		if (ItemStatUtils.isQuiver(item)) {
			return QuiverListener.getQuiverConfig(item);
		}
		return null;
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		ItemStack container = event.getCurrentItem();
		CustomContainerItemConfiguration config = getConfiguration(container);
		if (event.getClickedInventory() instanceof PlayerInventory // to prevent issues, only allow using from player inventory
			    && container != null
			    && config != null
			    && container.getAmount() == 1
			    && event.getWhoClicked() instanceof Player player) {
			ItemStack cursor = event.getCursor();
			if (cursor == null || cursor.getType() == Material.AIR) {
				if (event.getClick() == ClickType.RIGHT) {
					// open container
					event.setCancelled(true);
					if (!checkSoulbound(player, container) || !config.checkCanUse(player)) {
						return;
					}
					player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
					new CustomContainerItemGui(player, container, config).open();
				} else if (event.getClick() == ClickType.SWAP_OFFHAND && config.canQuickDeposit()) {
					// quick-fill container
					event.setCancelled(true);
					GUIUtils.refreshOffhand(event);
					if (!checkSoulbound(player, container) || !config.checkCanUse(player)) {
						return;
					}
					Map<String, Integer> depositedItems = new TreeMap<>();
					int deposited = 0;
					PlayerInventory inventory = player.getInventory();
					for (int i = 0; i < inventory.getSize(); i++) {
						ItemStack item = inventory.getItem(i);
						if (item != null && config.canPutIntoContainer(item)) {
							deposited += item.getAmount();
							depositedItems.merge(ItemUtils.getPlainNameOrDefault(item), item.getAmount(), Integer::sum);
							addToContainer(player, container, config, item, false, false);
						}
					}
					if (deposited > 0) {
						player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
						player.sendMessage(Component.text(deposited + " item" + (deposited == 1 ? "" : "s") + " deposited into " + ItemUtils.getPlainName(container), NamedTextColor.GOLD)
							                   .hoverEvent(HoverEvent.showText(Component.text(
								                   depositedItems.entrySet().stream().map(e -> e.getValue() + " " + e.getKey())
									                   .collect(Collectors.joining("\n")), NamedTextColor.GRAY))));
						ItemUpdateHelper.generateItemStats(container);
					}
				}
			} else {
				if (event.getClick() == ClickType.RIGHT) {
					event.setCancelled(true);
					if (!checkSoulbound(player, container) || !config.checkCanUse(player)) {
						return;
					}
					if (config.canPutIntoContainer(cursor)) {
						int oldAmount = cursor.getAmount();
						addToContainer(player, container, config, cursor, true, false);
						event.getView().setCursor(cursor);
						if (oldAmount != cursor.getAmount()) {
							player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
							player.sendMessage(Component.text("Item deposited into " + ItemUtils.getPlainName(container), NamedTextColor.GOLD));
						}
					} else {
						player.sendMessage(Component.text("This item cannot be put into a " + ItemUtils.getPlainName(container), NamedTextColor.RED));
						player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
					}
				}
			}
		}
	}

	private static boolean checkSoulbound(Player player, ItemStack item) {
		if (ItemStatUtils.getInfusionLevel(item, InfusionType.SOULBOUND) > 0
			    && !player.getUniqueId().equals(ItemStatUtils.getInfuser(item, InfusionType.SOULBOUND))) {
			player.sendMessage(Component.text("This " + ItemUtils.getPlainName(item) + " does not belong to you!", NamedTextColor.RED));
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			return false;
		}
		return true;
	}

	// Generates the container's lore text based on contents
	public static void generateDescription(ItemStack item, ReadableNBT monumenta, Consumer<Component> addLore) {
		CustomContainerItemConfiguration configuration = getConfiguration(item);
		if (configuration != null) {
			configuration.generateDescription(monumenta, addLore);
		}
	}

	public static boolean validateContainerItem(Player player, ItemStack container) {
		if (container.getAmount() != 1) {
			player.closeInventory();
			player.sendMessage(Component.text("Trying to be sneaky, are you?", NamedTextColor.RED));
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			return false;
		}
		for (ItemStack item : player.getInventory().getContents()) {
			if (NmsUtils.getVersionAdapter().isSameItem(item, container)) {
				return true;
			}
		}
		player.closeInventory();
		player.sendMessage(Component.text("Your " + container.getItemMeta().getDisplayName() + " has disappeared from your inventory.", NamedTextColor.RED));
		player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
		return false;
	}

	/**
	 * Adds as many of the given item to this container as possible. Modifies the given item stack amount to hold what did not fit.
	 */
	public static void addToContainer(Player player, ItemStack container, CustomContainerItemConfiguration config, ItemStack item, boolean generateItemStats, boolean silent) {
		if (!validateContainerItem(player, container)) {
			throw new IllegalStateException("Container not valid in addToContainer");
		}
		if (item.getType() == Material.AIR) {
			throw new IllegalArgumentException("Tried to add air to a container");
		}

		boolean updateItem = NBT.modify(container, nbt -> {
			ReadWriteNBTCompoundList itemsList = ItemStatUtils.getItemList(nbt);
			long totalCount = 0;
			ReadWriteNBT foundCompound = null;
			for (ReadWriteNBT compound : itemsList) {
				ItemStack containedItem = NBT.itemStackFromNBT(compound);
				ItemStatUtils.removePlayerModified(new NBTItem(containedItem, true));
				ReadableNBT playerModified = ItemStatUtils.addPlayerModified(compound.getCompound("tag"));
				if (playerModified == null) {
					continue;
				}
				// this can be null
				totalCount += playerModified.getLong(AMOUNT_KEY);
				if (containedItem.isSimilar(item)) {
					foundCompound = compound;
				}
			}

			if (config.mTotalItemsLimit > 0 && totalCount >= config.mTotalItemsLimit) {
				if (!silent) {
					player.sendMessage(Component.text("Cannot store any more items in this " + ItemUtils.getPlainName(container) + ".", NamedTextColor.RED));
					player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
				}
				return false;
			}

			if (foundCompound != null) {
				ReadWriteNBT playerModified = ItemStatUtils.addPlayerModified(foundCompound.getCompound("tag"));
				if (playerModified == null) {
					return false;
				}
				// this can be null
				Long existingAmount = playerModified.getLong(AMOUNT_KEY);
				int deposited = config.mTotalItemsLimit <= 0 ? item.getAmount() : (int) Math.min(item.getAmount(), config.mTotalItemsLimit - totalCount);
				if (config.mItemsPerTypeLimit > 0) {
					if (existingAmount >= config.mItemsPerTypeLimit) {
						if (!silent) {
							player.sendMessage(Component.text("Cannot store any more items of this type in this " + ItemUtils.getPlainName(container) + ".", NamedTextColor.RED));
							player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
						}
						return false;
					}
					deposited = (int) Math.min(deposited, config.mItemsPerTypeLimit - existingAmount);
				}
				playerModified.setLong(AMOUNT_KEY, existingAmount + deposited);
				item.setAmount(item.getAmount() - deposited);
				return true;
			}

			if (config.mTypesLimit > 0 && itemsList.size() >= config.mTypesLimit) {
				if (!silent) {
					player.sendMessage(Component.text("Cannot store any more different item types in this " + ItemUtils.getPlainName(container) + ".", NamedTextColor.RED));
					player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
				}
				return false;
			}

			ReadWriteNBT newCompound = itemsList.addCompound();
			ReadWriteNBT addedItem = NBT.itemStackToNBT(item);
			ItemStatUtils.addPlayerModified(addedItem.getOrCreateCompound("tag")).setLong(AMOUNT_KEY, (long) item.getAmount());
			addedItem.setByte("Count", (byte) 1);
			newCompound.mergeCompound(addedItem);
			item.setAmount(0);
			return true;
		});

		if (updateItem && generateItemStats) {
			ItemUpdateHelper.generateItemStats(container);
		}

	}

	static long countInContainer(Player player, ItemStack container, ItemStack currency) {
		if (!validateContainerItem(player, container)) {
			throw new IllegalStateException("Container not valid in countInContainer");
		}

		ReadableNBTList<ReadWriteNBT> itemsList = NBT.get(container, nbt -> {
			return ItemStatUtils.getItemList(nbt);
		});
		for (ReadWriteNBT compound : itemsList) {
			ReadWriteNBT playerModified = ItemStatUtils.getPlayerModified(compound.getOrCreateCompound("tag"));
			if (playerModified == null) {
				continue;
			}
			ItemStack containedItem = NBT.itemStackFromNBT(compound);
			NBT.modify(containedItem, ItemStatUtils::removePlayerModified);
			if (containedItem.isSimilar(currency)) {
				// this can be null
				return playerModified.getLong(AMOUNT_KEY);
			}
		}
		return 0;
	}

	/**
	 * Removes up to the specified amount of the given item from the container. The actually removed amount will be stored in the given item.
	 */
	public static void removeFromContainer(Player player, ItemStack container, ItemStack item) {
		if (!validateContainerItem(player, container)) {
			throw new IllegalStateException("Container not valid in removeFromContainer");
		}

		boolean found = NBT.modify(container, nbt -> {
			ReadWriteNBTCompoundList itemsList = ItemStatUtils.getItemList(nbt);
			for (int i = 0; i < itemsList.size(); i++) {
				ReadWriteNBT compound = itemsList.get(i);
				ItemStack containedItem = NBT.itemStackFromNBT(compound);
				NBT.modify(containedItem, inbt -> {
					ItemStatUtils.removePlayerModified(inbt);
				});
				if (containedItem.isSimilar(item)) {
					ReadWriteNBT playerModified = ItemStatUtils.addPlayerModified(compound.getOrCreateCompound("tag"));
					// this can be null
					long containedAmount = playerModified.getLong(AMOUNT_KEY);
					if (item.getAmount() >= containedAmount) {
						item.setAmount((int) containedAmount);
						itemsList.remove(i);
					} else {
						playerModified.setLong(AMOUNT_KEY, containedAmount - item.getAmount());
					}
					return true;
				}
			}
			return false;
		});

		if (found) {
			ItemUpdateHelper.generateItemStats(container);
			return;
		}

		item.setAmount(0);
	}

	public static @Nullable Pair<ItemStack, Boolean> removeFirstFromContainer(ItemStack container, int maxAmount, Predicate<ItemStack> testPredicate, Predicate<ItemStack> consumePredicate) {
		if (container.getAmount() != 1) { // don't perform most validity checks - this is called from outside of GUIs etc.
			throw new IllegalStateException("Container not valid in removeFirstFromContainer");
		}

		@Nullable Pair<ItemStack, Boolean> result = NBT.modify(container, nbt -> {
			ReadWriteNBTCompoundList itemsList = ItemStatUtils.getItemList(nbt);
			List<ReadWriteNBT> itemsListCopy = itemsList.toListCopy();
			for (int i = 0; i < itemsListCopy.size(); i++) {
				ReadWriteNBT compound = itemsList.get(i);
				ItemStack removedItem = NBT.itemStackFromNBT(compound);
				NBT.modify(removedItem, ItemStatUtils::removePlayerModified);
				if (!testPredicate.test(removedItem)) {
					continue;
				}
				ReadWriteNBT playerModified = ItemStatUtils.addPlayerModified(compound.getCompound("tag"));
				if (playerModified == null) {
					continue;
				}
				// this can be null
				long containedAmount = playerModified.getLong(AMOUNT_KEY);
				boolean consumed;
				if (maxAmount >= containedAmount) {
					removedItem.setAmount((int) containedAmount);
					consumed = consumePredicate.test(removedItem);
					if (consumed) {
						itemsList.remove(i);
					}
				} else {
					removedItem.setAmount(maxAmount);
					consumed = consumePredicate.test(removedItem);
					if (consumed) {
						playerModified.setLong(AMOUNT_KEY, containedAmount - removedItem.getAmount());
					}
				}
				return Pair.of(removedItem, consumed);
			}
			return null;
		});

		// only generate if consumed is true
		if (result != null && result.getValue()) {
			ItemUpdateHelper.generateItemStats(container);
		}

		return result;
	}

	static void reorderInContainer(Player player, ItemStack container, ItemStack item, int newIndex) {
		if (!validateContainerItem(player, container)) {
			throw new IllegalStateException("Container not valid in reorderInContainer");
		}

		NBT.modify(container, nbt -> {
			ReadWriteNBTCompoundList itemsList = ItemStatUtils.getItemList(nbt);

			for (int i = 0; i < itemsList.size(); i++) {
				ReadWriteNBT compound = itemsList.get(i);
				ItemStack containedItem = NBT.itemStackFromNBT(compound);
				NBT.modify(containedItem, ItemStatUtils::removePlayerModified);
				if (containedItem.isSimilar(item)) {
					// NBTCompoundList does not support inserting elements in the middle - so make an ArrayList and modify that, then copy the contents back
					List<ReadWriteNBT> list = itemsList.toListCopy();
					list.remove(i);
					list.add(Math.min(Math.max(0, newIndex), list.size()), compound);
					itemsList.clear();
					for (ReadWriteNBT newCompound : list) {
						itemsList.addCompound().mergeCompound(newCompound);
					}
					return;
				}
			}
		});

	}

	// When buying a container, it may get soulbound to the player that buys it (depending on the container config)
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void tradeWindowOpenEvent(TradeWindowOpenEvent event) {
		List<TradeWindowOpenEvent.Trade> trades = event.getTrades();
		for (int i = 0; i < trades.size(); i++) {
			TradeWindowOpenEvent.Trade trade = trades.get(i);
			MerchantRecipe recipe = trade.getRecipe();
			ItemStack result = recipe.getResult();
			CustomContainerItemConfiguration configuration = getConfiguration(result);
			if (configuration != null && configuration.soulBoundOnTrade() && trade.getOriginalResult() == null) {
				result = ItemUtils.clone(result);
				ItemStatUtils.addInfusion(result, InfusionType.SOULBOUND, 1, event.getPlayer().getUniqueId());
				MerchantRecipe newRecipe = new MerchantRecipe(result, recipe.getUses(), recipe.getMaxUses(), recipe.hasExperienceReward(), recipe.getVillagerExperience(),
					recipe.getPriceMultiplier(), recipe.getDemand(), recipe.getSpecialPrice(), recipe.shouldIgnoreDiscounts());
				newRecipe.setIngredients(recipe.getIngredients().stream().map(ItemUtils::clone).toList());
				TradeWindowOpenEvent.Trade newTrade = new TradeWindowOpenEvent.Trade(trade);
				newTrade.setRecipe(newRecipe);
				trades.set(i, newTrade);
			}
		}
	}

}
