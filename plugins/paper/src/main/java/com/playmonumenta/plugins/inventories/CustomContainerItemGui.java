package com.playmonumenta.plugins.inventories;

import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.SignUtils;
import de.tr7zw.nbtapi.NBTItem;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CustomContainerItemGui extends Gui {
	private final ItemStack mContainer;
	private final CustomContainerItemManager.CustomContainerItemConfiguration mConfig;
	private int mPage;

	public CustomContainerItemGui(Player player, ItemStack container, CustomContainerItemManager.CustomContainerItemConfiguration config) {
		super(player, (config.mTypesLimit <= 0 ? 6 : Math.min((config.mTypesLimit + 8) / 9, 5) + 1) * 9, container.getItemMeta().displayName());
		this.mContainer = container;
		this.mConfig = config;
		setFiller(Material.BLACK_STAINED_GLASS_PANE);
	}

	public ItemStack getContainer() {
		return mContainer;
	}

	@Override
	protected void setup() {
		if (!CustomContainerItemManager.validateContainerItem(mPlayer, mContainer)) {
			return;
		}

		List<ItemStack> items = new ArrayList<>(
			ItemStatUtils.addPlayerModified(new NBTItem(mContainer))
				.getCompoundList(ItemStatUtils.ITEMS_KEY).stream()
				.map(NBTItem::convertNBTtoItem)
				.toList());

		// Fill GUI with items
		boolean showAmounts = mPlayer.getScoreboardTags().contains(CustomContainerItemManager.SHOW_AMOUNTS_TAG);
		boolean showAmountsAsStacks = mPlayer.getScoreboardTags().contains(CustomContainerItemManager.SHOW_AMOUNTS_AS_STACKS_TAG);
		int pos = 0;
		int itemsPerPage = mSize - 9; // top row reserved
		for (ItemStack item : items) {
			int posInPage = pos - itemsPerPage * mPage;
			if (posInPage < 0 || posInPage >= itemsPerPage) {
				pos++;
				continue;
			}
			ItemStack displayItem = ItemUtils.clone(item);
			ItemMeta itemMeta = displayItem.getItemMeta();
			long amount = ItemStatUtils.addPlayerModified(new NBTItem(item)).getLong(CustomContainerItemManager.AMOUNT_KEY);
			String amountString;
			if (showAmountsAsStacks && item.getMaxStackSize() > 1 && amount >= item.getMaxStackSize()) {
				long stacks = amount / item.getMaxStackSize();
				long remaining = amount % item.getMaxStackSize();
				amountString = amount + " (" + stacks + " stack" + (stacks == 1 ? "" : "s") + (remaining == 0 ? "" : " + " + remaining) + ")";
			} else {
				amountString = "" + amount;
			}
			itemMeta.displayName(Component.text(amountString + " ", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)
				                     .append(ItemUtils.getDisplayName(item).colorIfAbsent(NamedTextColor.WHITE)));
			displayItem.setItemMeta(itemMeta);
			if (showAmounts) {
				displayItem.setAmount((int) Math.max(1, Math.min(64, showAmountsAsStacks ? amount / item.getMaxStackSize() : amount)));
			}
			setItem(1 + posInPage / 9, posInPage % 9, new GuiItem(displayItem, false))
				.onClick(event -> {
					ItemStack movedItem = ItemUtils.clone(item);
					ItemStatUtils.removePlayerModified(new NBTItem(movedItem, true));
					switch (event.getClick()) {
						case LEFT -> {
							movedItem.setAmount((int) Math.min(movedItem.getMaxStackSize(), amount));
							CustomContainerItemManager.removeFromContainer(mPlayer, mContainer, movedItem);
							mPlayer.setItemOnCursor(movedItem);
							update();
						}
						case RIGHT -> {
							movedItem.setAmount(1);
							CustomContainerItemManager.removeFromContainer(mPlayer, mContainer, movedItem);
							mPlayer.setItemOnCursor(movedItem);
							update();
						}
						case SHIFT_LEFT, SHIFT_RIGHT -> {
							int maxFit = InventoryUtils.numCanFitInInventory(movedItem, mPlayer.getInventory());
							if (maxFit > 0) {
								movedItem.setAmount(event.getClick() == ClickType.SHIFT_LEFT ? Math.min(movedItem.getMaxStackSize(), maxFit) : maxFit);
								CustomContainerItemManager.removeFromContainer(mPlayer, mContainer, movedItem);
								mPlayer.getInventory().addItem(movedItem);
							}
							update();
						}
						case SWAP_OFFHAND -> {
							close();
							SignUtils.newMenu(List.of("", "~~~~~~~~~~~", "Enter how many", "items to retrieve"))
								.response((player, lines) -> {
									double retrievedAmount;
									try {
										retrievedAmount = lines[0].isEmpty() ? 0 : WalletManager.parseDoubleOrCalculation(lines[0]);
									} catch (NumberFormatException e) {
										player.sendMessage(Component.text("Please enter a valid number.", NamedTextColor.RED));
										return false;
									}
									if (retrievedAmount < 0) {
										player.sendMessage(Component.text("Please enter a positive number.", NamedTextColor.RED));
										return false;
									}
									long desiredAmount = (long) Math.ceil(retrievedAmount);
									long countInContainer = CustomContainerItemManager.countInContainer(mPlayer, mContainer, movedItem);

									// Warn if not enough and exit (to not take out less than expected if not double-checked)
									if (desiredAmount > countInContainer) {
										player.sendMessage(Component.text("There are fewer than the requested amount of items in the bag.", NamedTextColor.RED));
										player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
										open();
										return true;
									}
									if (desiredAmount >= Integer.MAX_VALUE) { // no. even if you really have that many.
										return false;
									}

									if (desiredAmount <= InventoryUtils.numCanFitInInventory(movedItem, mPlayer.getInventory())) {
										movedItem.setAmount((int) desiredAmount);
										CustomContainerItemManager.removeFromContainer(mPlayer, mContainer, movedItem);
										mPlayer.getInventory().addItem(movedItem);
									} else {
										player.sendMessage(Component.text("Not enough space in inventory for all items. No items have been retrieved.", NamedTextColor.RED));
										player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
									}
									open();
									return true;
								})
								.reopenIfFail(true)
								.open(mPlayer);
						}
						case DROP, CONTROL_DROP -> {
							int dropAmount = event.getClick() == ClickType.CONTROL_DROP ? 64 : 1;
							movedItem.setAmount(dropAmount);
							CustomContainerItemManager.removeFromContainer(mPlayer, mContainer, movedItem);
							Item itemEntity = mPlayer.getWorld().dropItem(mPlayer.getEyeLocation(), movedItem);
							itemEntity.setVelocity(mPlayer.getLocation().getDirection());
							update();
						}
						default -> {
							// Are you happy now, PMD?
						}
					}
				});
			pos++;
		}

		// page arrows and info item
		if (mPage > 0) {
			ItemStack previousPageIcon = new ItemStack(Material.ARROW);
			ItemMeta itemMeta = previousPageIcon.getItemMeta();
			itemMeta.displayName(Component.text("Previous Page", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
			previousPageIcon.setItemMeta(itemMeta);
			setItem(0, previousPageIcon)
				.onLeftClick(() -> {
					mPage--;
					update();
				});
		}
		{
			ItemStack infoIcon = new ItemStack(Material.DARK_OAK_SIGN);
			ItemMeta itemMeta = infoIcon.getItemMeta();
			itemMeta.displayName(Component.text(ItemUtils.getPlainName(mContainer) + " Info", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
			itemMeta.lore(List.of(
				Component.text("Click here to toggle displaying item counts.", NamedTextColor.WHITE)
					.decoration(TextDecoration.ITALIC, false),
				Component.text("Right click here to toggle showing counts in stacks.", NamedTextColor.WHITE)
					.decoration(TextDecoration.ITALIC, false),
				Component.text("Shift + Right Click items to move all of the same type.", NamedTextColor.GRAY)
					.decoration(TextDecoration.ITALIC, false),
				Component.text("Press ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
					.append(Component.keybind("key.swapOffhand", NamedTextColor.WHITE))
					.append(Component.text(" on an item to retrieve a custom amount.", NamedTextColor.GRAY))
			));
			infoIcon.setItemMeta(itemMeta);
			setItem(4, infoIcon)
				.onLeftClick(() -> {
					ScoreboardUtils.toggleTag(mPlayer, CustomContainerItemManager.SHOW_AMOUNTS_TAG);
					update();
				})
				.onRightClick(() -> {
					ScoreboardUtils.toggleTag(mPlayer, CustomContainerItemManager.SHOW_AMOUNTS_AS_STACKS_TAG);
					update();
				});
		}
		if (pos > itemsPerPage * (mPage + 1)) {
			ItemStack nextPageIcon = new ItemStack(Material.ARROW);
			ItemMeta itemMeta = nextPageIcon.getItemMeta();
			itemMeta.displayName(Component.text("Next Page", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
			nextPageIcon.setItemMeta(itemMeta);
			setItem(8, nextPageIcon)
				.onLeftClick(() -> {
					mPage++;
					update();
				});
		}

		mConfig.createAdditionalGuiItems(mContainer, this);

	}

	@Override
	protected boolean onGuiClick(InventoryClickEvent event) {
		if (!CustomContainerItemManager.validateContainerItem(mPlayer, mContainer)) {
			return false;
		}

		ItemStack cursor = event.getCursor();
		if (cursor != null && cursor.getType() != Material.AIR) {
			switch (event.getClick()) {
				case LEFT, RIGHT -> {
					handleContainerClick(cursor, event.getSlot());
				}
				default -> {
					// Cancel all other clicks with an item on the cursor
					mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
				}
			}
			return false;
		}

		return true;
	}

	private void handleContainerClick(ItemStack cursor, int slot) {
		if (slot < 9 || !mConfig.canPutIntoContainer(cursor)) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			return;
		}
		int itemsPerPage = mSize - 9;
		int position = slot - 9 + mPage * itemsPerPage;
		ItemStack clone = ItemUtils.clone(cursor);
		CustomContainerItemManager.addToContainer(mPlayer, mContainer, mConfig, cursor, false, false);
		CustomContainerItemManager.reorderInContainer(mPlayer, mContainer, clone, position);
		ItemStatUtils.generateItemStats(mContainer);
		update();
	}

	@Override
	protected void onOutsideInventoryClick(InventoryClickEvent event) {
		// allow dropping items as normal
		switch (event.getClick()) {
			case LEFT, RIGHT, CREATIVE, DROP, CONTROL_DROP, WINDOW_BORDER_LEFT, WINDOW_BORDER_RIGHT -> {
				if (event.getSlotType() == InventoryType.SlotType.OUTSIDE) {
					event.setCancelled(false);
				}
			}
			default -> {
				// keep cancelled
			}
		}
	}

	@Override
	protected void onPlayerInventoryClick(InventoryClickEvent event) {
		if (!CustomContainerItemManager.validateContainerItem(mPlayer, mContainer)) {
			return;
		}
		switch (event.getClick()) {
			case SHIFT_LEFT -> {
				ItemStack currentItem = event.getCurrentItem();
				if (currentItem != null && mConfig.canPutIntoContainer(currentItem)) {
					CustomContainerItemManager.addToContainer(mPlayer, mContainer, mConfig, currentItem, true, false);
					update();
				} else if (!ItemUtils.isNullOrAir(currentItem)) {
					mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
				}
			}
			case SHIFT_RIGHT -> {
				ItemStack currentItem = event.getCurrentItem();
				if (currentItem != null && mConfig.canPutIntoContainer(currentItem)) {
					ItemStack clickedItem = ItemUtils.clone(currentItem);
					CustomContainerItemManager.addToContainer(mPlayer, mContainer, mConfig, currentItem, false, false);
					if (currentItem.getAmount() == 0) {
						for (ItemStack item : mPlayer.getInventory().getStorageContents()) {
							if (item != null && item.isSimilar(clickedItem)) {
								CustomContainerItemManager.addToContainer(mPlayer, mContainer, mConfig, item, false, false);
								if (item.getAmount() != 0) {
									break;
								}
							}
						}
					}
					if (clickedItem.getAmount() != currentItem.getAmount()) {
						ItemStatUtils.generateItemStats(mContainer);
					}
					update();
				} else if (!ItemUtils.isNullOrAir(currentItem)) {
					mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
				}
			}
			case LEFT, RIGHT, CREATIVE, DROP, CONTROL_DROP, WINDOW_BORDER_LEFT, WINDOW_BORDER_RIGHT, MIDDLE, SWAP_OFFHAND, NUMBER_KEY -> {
				// These clicks don't modify the container inventory, so allow them - as long as the click is not on the open container item itself
				if (!NmsUtils.getVersionAdapter().isSameItem(event.getCurrentItem(), mContainer)) {
					event.setCancelled(false);
				} else {
					mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
				}
			}
			default -> {
				// Keep event cancelled otherwise (double click, unknown/modded click)
				mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			}
		}
	}

	@Override
	protected void onInventoryDrag(InventoryDragEvent event) {
		// Dragging in the player inventory only is allowed.
		if (event.getRawSlots().stream().allMatch(slot -> event.getView().getInventory(slot) == mPlayer.getInventory())) {
			event.setCancelled(false);
			return;
		}

		// Dragging an item over one slot in the GUI: handle like a left click
		ItemStack cursor = event.getCursor();
		if (cursor != null && cursor.getType() != Material.AIR
			    && event.getRawSlots().size() == 1
			    && event.getView().getInventory(event.getRawSlots().iterator().next()) != mPlayer.getInventory()) {
			handleContainerClick(cursor, event.getRawSlots().iterator().next());
		}
	}

}
