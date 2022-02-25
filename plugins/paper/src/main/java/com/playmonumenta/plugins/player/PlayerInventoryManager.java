package com.playmonumenta.plugins.player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.listeners.ShulkerEquipmentListener;
import com.playmonumenta.plugins.utils.InventoryUtils;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class PlayerInventoryManager {
	/*
	 * This list contains all of a player's currently valid item properties,
	 * including ones that are on duplicate specialized lists below
	 * Contains the hashmaps in a hashmap that represents the inventory slots
	 *
	 * mInventoryProperties indexes 0-40 for inventory slots and custom enchants.
	 * 0-8 = hotbar, 36-39 = armor, 40 = offhand
	 *
	 * Needs to be ordered so that trigger orders are correct
	 */

	private final @Nullable ItemStack[] mInventoryLastCheck = new ItemStack[41];

	//Set true when player shift clicks items in inventory so it only runs after inventory is closed
	private boolean mNeedsUpdate = false;

	public PlayerInventoryManager(Plugin plugin, Player player) {
		InventoryUtils.scheduleDelayedEquipmentCheck(plugin, player, new PlayerJoinEvent(player, Component.text(""))); // Just a dummy event
	}

	//Updates only for the slot given
	public void updateItemSlotProperties(Plugin plugin, Player player, int slot) {
		@Nullable ItemStack[] inv = player.getInventory().getContents();
		if (slot < 0 || slot >= inv.length) {
			return;
		}
		updateItemLastCheck(slot, inv[slot]);
	}

	public void updateItemLastCheck(int slot, @Nullable ItemStack item) {
		if (item == null) {
			mInventoryLastCheck[slot] = null;
		} else {
			mInventoryLastCheck[slot] = item.clone();
		}
	}

	public void updateEquipmentProperties(Plugin plugin, Player player, @Nullable Event event) {
		// Updates different indexes for custom enchant depending on the event given, if null or not listed, rescan everything
		if (event instanceof InventoryClickEvent) {
			InventoryClickEvent invClickEvent = (InventoryClickEvent) event;
			if (invClickEvent.getSlotType() == InventoryType.SlotType.CRAFTING
				|| invClickEvent.isShiftClick() || invClickEvent.getSlot() == -1) {
				mNeedsUpdate = true;
				return;
			} else if (invClickEvent.isRightClick() && ShulkerEquipmentListener.isEquipmentBox(invClickEvent.getCurrentItem())) {
				for (int i = 0; i <= 8; i++) {
					// Update hotbar
					updateItemSlotProperties(plugin, player, i);
				}
				for (int i = 36; i <= 40; i++) {
					// Update armor and offhand
					updateItemSlotProperties(plugin, player, i);
				}
			} else if (invClickEvent.getHotbarButton() != -1) {
				// Updates clicked slot and hotbar slot if numbers were used to swap
				updateItemSlotProperties(plugin, player, invClickEvent.getSlot());
				updateItemSlotProperties(plugin, player, invClickEvent.getHotbarButton());
			} else if (invClickEvent.getClick().equals(ClickType.SWAP_OFFHAND)) {
				// Updates clicked slot and offhand slot when swap hands key is used
				updateItemSlotProperties(plugin, player, invClickEvent.getSlot());
				updateItemSlotProperties(plugin, player, 40);
			} else {
				updateItemSlotProperties(plugin, player, invClickEvent.getSlot());
			}
		} else if (event instanceof InventoryDragEvent inventoryDragEvent) {
			for (int i : inventoryDragEvent.getInventorySlots()) {
				updateItemSlotProperties(plugin, player, i);
			}
		} else if (event instanceof PlayerInteractEvent || event instanceof BlockDispenseArmorEvent) {
			for (int i = 36; i <= 39; i++) {
				// Update armor
				updateItemSlotProperties(plugin, player, i);
			}
		} else if (event instanceof PlayerItemBreakEvent) {
			//Updates item properties for armor, mainhand and offhand
			updateItemSlotProperties(plugin, player, player.getInventory().getHeldItemSlot());
			for (int i = 36; i <= 40; i++) {
				// Update armor and offhand
				updateItemSlotProperties(plugin, player, i);
			}
		} else if (event instanceof PlayerItemHeldEvent) {
			updateItemSlotProperties(plugin, player, ((PlayerItemHeldEvent) event).getPreviousSlot());
			updateItemSlotProperties(plugin, player, ((PlayerItemHeldEvent) event).getNewSlot());
		} else if (event instanceof PlayerSwapHandItemsEvent) {
			updateItemSlotProperties(plugin, player, player.getInventory().getHeldItemSlot());
			updateItemSlotProperties(plugin, player, 40);
		} else if (event instanceof PlayerDropItemEvent playerDropItemEvent) {
			int heldItemSlot = player.getInventory().getHeldItemSlot();
			if (hasSlotChanged(player, heldItemSlot)) {
				updateItemSlotProperties(plugin, player, heldItemSlot);
			} else {
				int droppedSlot = getDroppedSlotId(playerDropItemEvent);
				updateItemSlotProperties(plugin, player, droppedSlot);
			}
		} else if (!mNeedsUpdate && event instanceof InventoryCloseEvent) {
			return; //Only ever updates on InventoryCloseEvent if shift clicks have been made
		} else {

			// Sets mHasShiftClicked to false after updating entire inventory
			if (mNeedsUpdate && event instanceof InventoryCloseEvent) {
				mNeedsUpdate = false;
			}
		}
	}

	public boolean hasSlotChanged(Player player, int slot) {
		if (slot < 0 || slot > 40) {
			return false;
		}
		@Nullable ItemStack oldItem = mInventoryLastCheck[slot];
		@Nullable ItemStack currentItem = player.getInventory().getContents()[slot];
		return !Objects.equals(oldItem, currentItem);
	}

	// Returns the first similar slot's number where there is a difference in item count, or -1 if not found
	public int getDroppedSlotId(PlayerDropItemEvent event) {
		Player player = event.getPlayer();
		@Nullable ItemStack[] inv = player.getInventory().getContents();
		ItemStack droppedItem = event.getItemDrop().getItemStack();

		for (int slot = 0; slot <= 40; slot++) {
			@Nullable ItemStack oldItem = mInventoryLastCheck[slot];
			if (oldItem == null || !droppedItem.isSimilar(oldItem)) {
				continue;
			}
			int oldAmount = oldItem.getAmount();

			@Nullable ItemStack currentItem = inv[slot];
			if (currentItem != null && droppedItem.isSimilar(currentItem)) {
				int newAmount = currentItem.getAmount();
				if (oldAmount - newAmount > 0) {
					return slot;
				}
			} else {
				return slot;
			}
		}
		return -1;
	}
}
