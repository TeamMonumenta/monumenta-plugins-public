package com.playmonumenta.plugins.listeners;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.CurseOfEphemerality;
import com.playmonumenta.plugins.utils.ItemUtils;

import net.md_5.bungee.api.ChatColor;

/**
 * These listeners work together with ShulkerInventoryManager and ShulkerInventory to
 * Allow players to access Shulker Boxes without being placed first.
 *
 * @see com.playmonumenta.plugins.inventories.ShulkerInventoryManager
 * @see com.playmonumenta.plugins.inventories.ShulkerInventory
 */
public class ShulkerShortcutListener implements Listener {
	private static final Permission PERMISSION = new Permission("monumenta.feature.shulkershortcut");
	private final Plugin mPlugin;

	public ShulkerShortcutListener(Plugin plugin) {
		mPlugin = plugin;
	}

	/**
	 * Event Handler for when a player performs any click inside any inventory.
	 * Used to prevent open Shulker Boxes from being removed from an inventory,
	 * as well as to open Shulker Boxes or deposit items when right-clicked.
	 *
	 * @see InventoryClickEvent
	 */
	@EventHandler(priority = EventPriority.HIGH)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (event.isCancelled()) {
			return;
		}
		ClickType click = event.getClick();
		InventoryAction action = event.getAction();
		ItemStack itemHeld = event.getCursor();
		ItemStack itemClicked = event.getCurrentItem();
		Inventory topInventory = event.getView().getTopInventory();
		Inventory clickedInventory = event.getClickedInventory();
		if (event.getWhoClicked() instanceof Player && clickedInventory != null) {
			// A player clicked inside an inventory window
			Player player = (Player)event.getWhoClicked();
			if (topInventory.getType() == InventoryType.SHULKER_BOX &&           // Player has Shulker open
			    mPlugin.mShulkerInventoryManager.playerHasShulkerOpen(player) && // Shulker was opened via shortcut
			    !mPlugin.mShulkerInventoryManager.updateShulker(player)) {       // Try to update Shulker if it still exists.
				// The currently open shulker no longer exists, cancel the click and close the inventory.
				event.setCancelled(true);
				player.sendMessage(ChatColor.DARK_RED + "Shulker no longer available");
				new BukkitRunnable() {
					@Override
					public void run() {
						player.closeInventory(InventoryCloseEvent.Reason.CANT_USE);
					}
				}.runTask(mPlugin);
			} else if (itemClicked != null && ItemUtils.isShulkerBox(itemClicked.getType()) &&
			           !ShulkerEquipmentListener.isEquipmentBox(itemClicked) &&
			           !ItemUtils.isItemShattered(itemClicked)) {
				// Player clicked a non-shattered non-equipment shulker box in an inventory.
				if (mPlugin.mShulkerInventoryManager.isShulkerInUse(itemClicked)) {
					// A currently open shulker box was clicked, cancel.
					player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
					player.sendMessage(ChatColor.DARK_RED + "That shulker is open");
					event.setCancelled(true);
				} else {
					// A shulker box that isn't currently open was clicked.
					if (player.hasPermission(PERMISSION)) {
						if (click == ClickType.RIGHT && action == InventoryAction.SWAP_WITH_CURSOR &&
							itemHeld != null && !ItemUtils.isShulkerBox(itemHeld.getType()) &&
							!CurseOfEphemerality.isEphemeral(itemHeld)) {

							// Player right-clicked shulker while holding an item on their cursor.
							event.setCancelled(true);
							int starting = itemHeld.getAmount();
							int remaining = mPlugin.mShulkerInventoryManager.addItemToShulker(player, clickedInventory, itemClicked, itemHeld);
							switch (remaining) {
								case -3:
									// Somehow that wasn't a shulker
									player.sendMessage(String.format("%s%sHow did you...? That isn't a shulker. Please report this", ChatColor.DARK_RED, ChatColor.BOLD));
									player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
									break;
								case -2:
									// Shulker is locked
									player.sendMessage(String.format("%s%sThat shulker is locked", ChatColor.DARK_RED, ChatColor.BOLD));
									player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
									break;
								case -1:
									// Shulker is already open
									player.sendMessage(String.format("%s%sThat shulker is already open", ChatColor.DARK_RED, ChatColor.BOLD));
									player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
									break;
								case 0:
									// All items were inserted successfully.
									player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
									player.sendMessage(ChatColor.GOLD + "Item deposited into shulker.");
									event.getView().setCursor(null);
									break;
								default:
									if (remaining == starting) {
										// No items were placed, shulker is full.
										player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
										player.sendMessage(ChatColor.DARK_RED + "That shulker is full.");
									} else {
										// Items were inserted, but not all
										player.sendMessage(ChatColor.DARK_RED + "That shulker was too full to accept the full stack.");
										player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
									}
							}
						} else if (click == ClickType.RIGHT && action == InventoryAction.PICKUP_HALF) {
							// Player right-clicked shulker with an empty cursor.
							if (mPlugin.mShulkerInventoryManager.openShulker(player, clickedInventory, itemClicked)) {
								// Shulker was successfully opened, cancel.
								player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
								event.setCancelled(true);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Event Handler for when the player drags an ItemStack across one or more slots in an inventory.
	 * Used to update Shulker Boxes when items are dragged in their inventory.
	 *
	 * @see InventoryDragEvent
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void inventoryDragEvent(InventoryDragEvent event) {
		if (event.getWhoClicked() instanceof Player) {
			Player player = (Player)event.getWhoClicked();
			if (event.getView().getTopInventory().getType() == InventoryType.SHULKER_BOX && // Player has Shulker open
				mPlugin.mShulkerInventoryManager.playerHasShulkerOpen(player) && // Shulker was opened via shortcut
				!mPlugin.mShulkerInventoryManager.updateShulker(player)) { // Try to update Shulker if it still exists.
				// The currently open shulker no longer exists, cancel the click and close the inventory.
				event.setCancelled(true);
				player.sendMessage(ChatColor.DARK_RED + "Shulker no longer available");
				new BukkitRunnable() {
					@Override
					public void run() {
						player.closeInventory(InventoryCloseEvent.Reason.CANT_USE);
					}
				}.runTask(mPlugin);
			}
		}
	}

	/**
	 * Event Handler for whenever an inventory is closed.
	 * Used to update and unlock Shulker Boxes when closed.
	 *
	 * @see InventoryCloseEvent
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void inventoryCloseEvent(InventoryCloseEvent event) {
		if (event.getPlayer() instanceof Player) {
			Player player = (Player)event.getPlayer();
			mPlugin.mShulkerInventoryManager.closeShulker(player);
		}
	}

	/**
	 * Event Handler for whenever a dispenser is activated.
	 * Used to prevent placing open shulkers, and unlock shulkers with an invalid lock.
	 *
	 * @see BlockDispenseEvent
	 */
	@EventHandler(priority = EventPriority.LOW)
	public void blockDispenseEvent(BlockDispenseEvent event) {
		if (!event.isCancelled() &&
			ItemUtils.isShulkerBox(event.getItem().getType()) &&
			mPlugin.mShulkerInventoryManager.isShulkerInUse(event.getItem())) {
			event.setCancelled(true);
		}
	}

	/**
	 * Event Handler for whenever a block is placed.
	 * Used to prevent placing open shulkers, and unlock shulkers with an invalid lock.
	 *
	 * @see BlockPlaceEvent
	 */
	@EventHandler(priority = EventPriority.LOW)
	public void blockPlaceEvent(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		Block block = event.getBlockPlaced();
		if (!event.isCancelled() &&
		    ItemUtils.isShulkerBox(block.getType()) &&
		    mPlugin.mShulkerInventoryManager.isShulkerInUse(block)) {
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			player.sendMessage(ChatColor.DARK_RED + "That shulker is open");
			event.setCancelled(true);
			event.setBuild(false);
		}
	}
}
