package com.playmonumenta.plugins.inventories;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class IndexInventoryListeners implements Listener {
	@EventHandler(priority = EventPriority.NORMAL)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (event.isCancelled()) {
			return;
		}
		Inventory clickedInventory = event.getClickedInventory();
		int slot = event.getSlot();
		if (event.getWhoClicked() instanceof Player && clickedInventory != null) {
			// A player clicked inside an inventory window
			Player player = (Player) event.getWhoClicked();
			IndexInventoryManager manager = Plugin.getInstance().mIndexInventoryManager;
			if (manager.isPlayersIndexInventory(player, clickedInventory)) {
				// the clicked inventory is the player's item index instance
				event.setCancelled(true);
				switch (slot) {
					case 53:
						manager.pageUp(player);
						break;
					case 52:
						manager.pageDown(player);
						break;
					case 50:
						manager.resetFilters(player);
						break;
					default:
						manager.pickItem(player, slot);
						break;
				}
			}
		}
	}
}
