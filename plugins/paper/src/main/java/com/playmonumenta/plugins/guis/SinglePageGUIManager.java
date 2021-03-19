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
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.guis.singlepageguis.ExampleSinglePageGUI;
import com.playmonumenta.plugins.guis.singlepageguis.OrinSinglePageGUI;

public class SinglePageGUIManager implements Listener {

	protected static Plugin mPlugin;

	public SinglePageGUIManager(Plugin plugin) {
		// Whenever you make a SinglePageGUI, it must be registered here
		new ExampleSinglePageGUI(null, null).registerCommand();
		new OrinSinglePageGUI(null, null).registerCommand();
		mPlugin = plugin;
	}

	private static final Map<UUID, SinglePageGUI> GUI_MAPPINGS = new HashMap<>();

	public static void openGUI(Player player, SinglePageGUI gui) {
		UUID uuid = player.getUniqueId();
        if (!GUI_MAPPINGS.containsKey(uuid)) {
            GUI_MAPPINGS.put(uuid, gui);
            gui.openGUI();
            new BukkitRunnable() {
				@Override
				public void run() {
					if (player.getOpenInventory() == null || player.getOpenInventory().getTopInventory() == null) {
						GUI_MAPPINGS.remove(player.getUniqueId());
					}
				}
            }.runTaskLater(mPlugin, 20);
        }
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
