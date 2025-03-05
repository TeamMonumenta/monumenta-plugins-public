package com.playmonumenta.plugins.guis.lib;

import com.playmonumenta.plugins.utils.GUIUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class GuiListener implements Listener {
	@EventHandler(ignoreCancelled = false)
	protected void inventoryClick(InventoryClickEvent event) {
		if (!(event.getInventory().getHolder(false) instanceof Gui gui)) {
			return;
		}

		event.setCancelled(true);
		GUIUtils.refreshOffhand(event);

		if (event.getClickedInventory() == gui.getInventory()) {
			if (gui.onGuiClick(event) && event.getSlot() < gui.mItems.size()) {
				GuiItem item = gui.mItems.get(event.getSlot());
				if (item != null) {
					item.handleClicked(event);
				}
			}
		} else if (event.getClickedInventory() != null) {
			gui.onPlayerInventoryClick(event);
		} else {
			gui.onOutsideInventoryClick(event);
		}

		gui.update();
	}

	@EventHandler(ignoreCancelled = false)
	protected void inventoryDrag(InventoryDragEvent event) {
		if (!(event.getInventory().getHolder(false) instanceof Gui gui)) {
			return;
		}

		event.setCancelled(true);
		gui.onInventoryDrag(event);

		gui.update();
	}

	@EventHandler(ignoreCancelled = false)
	protected void inventoryClose(InventoryCloseEvent event) {
		if (!(event.getInventory().getHolder(false) instanceof Gui gui)) {
			return;
		}

		gui.onClose(event);
	}
}
