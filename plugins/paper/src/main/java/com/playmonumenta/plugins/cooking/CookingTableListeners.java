package com.playmonumenta.plugins.cooking;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class CookingTableListeners implements Listener {
	private final Plugin mPlugin;

	public CookingTableListeners(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler
	public void inventoryCloseEvent(InventoryCloseEvent event) {
		Inventory inv = event.getInventory();
		Player player = (Player)event.getPlayer();
		if (mPlugin.mCookingTableInventoryManager.isPlayerCookingHUD(player, inv)) {
			mPlugin.mCookingTableInventoryManager.closeTable(player.getUniqueId());
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (event.isCancelled()) {
			return;
		}
		ClickType click = event.getClick();
		ItemStack itemHeld = event.getCursor();
		ItemStack itemClicked = event.getCurrentItem();
		Inventory topInventory = event.getView().getTopInventory();
		Inventory clickedInventory = event.getClickedInventory();
		int slot = event.getSlot();
		if (event.getWhoClicked() instanceof Player && clickedInventory != null) {
			// A player clicked inside an inventory window
			Player player = (Player) event.getWhoClicked();
			if (mPlugin.mCookingTableInventoryManager.isPlayerCookingHUD(player, clickedInventory)) {
				// the clicked inventory is the player's cooking table HUD
				if (itemClicked != null && (
					itemClicked.equals(CookingConsts.BASE_TABLE_CONTENTS_NYU) ||
					itemClicked.equals(CookingConsts.BASE_TABLE_CONTENTS_BASE) ||
					itemClicked.equals(CookingConsts.BASE_TABLE_CONTENTS_BG) ||
					itemClicked.equals(CookingConsts.BASE_TABLE_CONTENTS_ING) ||
					itemClicked.equals(CookingConsts.BASE_TABLE_CONTENTS_OUT) ||
					itemClicked.equals(CookingConsts.BASE_TABLE_CONTENTS_TOOL) ||
					itemClicked.getItemMeta().getDisplayName().equals(CookingConsts.ERROR_ITEM_NAME))) {
					// if the clicked item is part of the HUD, cancel the event
					event.setCancelled(true);
				} else if (slot == 25) {
					if (itemHeld == null || itemHeld.getType() == Material.AIR) {
						if (click == ClickType.SHIFT_LEFT) {
							// shift clicked from the output slot
							mPlugin.mCookingTableInventoryManager.cookAll(player);
							mPlugin.mCookingTableInventoryManager.updateTable(player);
							event.setCancelled(true);
						} else {
							// simple click from the output slot
							mPlugin.mCookingTableInventoryManager.cookOne(player);
							mPlugin.mCookingTableInventoryManager.updateTable(player);
							event.setCancelled(true);
						}
					} else {
						event.setCancelled(true);
					}
				} else {
					// else, the player must have clicked an empty space, or a player-placed item.
					// if so, that means the table recipe just changed, update the preview
					mPlugin.mCookingTableInventoryManager.updateTable(player);
				}
			} else if ((click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) &&
				mPlugin.mCookingTableInventoryManager.isPlayerCookingHUD(player, topInventory)) {
				//shift clicked into a cooking table
				mPlugin.mCookingTableInventoryManager.updateTable(player);
			}
		}
	}
}
