package com.playmonumenta.plugins.guis;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;

import com.playmonumenta.plugins.guis.singlepageguis.ExampleSinglePageGUI;

public class SinglePageGUIManager implements Listener {

	public SinglePageGUIManager() {
		// Whenever you make a SinglePageGUI, it must be registered here
		new ExampleSinglePageGUI(null, null).registerCommand();
	}

	private static final Map<UUID, SinglePageGUI> GUI_MAPPINGS = new HashMap<>();

	public static void openGUI(Player player, SinglePageGUI gui) {
		GUI_MAPPINGS.put(player.getUniqueId(), gui);
		gui.openGUI();
	}

	private SinglePageGUI getGUI(InventoryInteractEvent event) {
		HumanEntity entity = event.getWhoClicked();
		if (entity instanceof Player) {
			Player player = (Player) entity;

			SinglePageGUI gui = GUI_MAPPINGS.get(player.getUniqueId());
			if (gui != null && gui.contains(event.getInventory())) {
				return gui;
			}
		}

		return null;
	}

	@EventHandler
	public void inventoryClickEvent(InventoryClickEvent event) {
		SinglePageGUI gui = getGUI(event);
		if (gui != null) {
			gui.registerClick(event);
			event.setCancelled(true);
		}
    }

	@EventHandler
	public void inventoryInteractEvent(InventoryInteractEvent event) {
		if (getGUI(event) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void inventoryCloseEvent(InventoryCloseEvent event) {
		GUI_MAPPINGS.remove(event.getPlayer().getUniqueId());
	}

}
