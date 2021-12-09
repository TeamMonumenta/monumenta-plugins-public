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
import org.bukkit.event.player.PlayerQuitEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.guis.singlepageguis.ExampleSinglePageGUI;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;

public class SinglePageGUIManager implements Listener {

	private static final Map<UUID, SinglePageGUI> GUI_MAPPINGS = new HashMap<>();
	private static final Map<UUID, Integer> LAST_TICK_OPENED = new HashMap<>();

	private static final int TIMEOUT = 20 * 1;

	public SinglePageGUIManager() {
		// Whenever you make a SinglePageGUI, it must be registered here
		new ExampleSinglePageGUI(null, null).registerCommand();
	}

	public static void openGUI(Player player, SinglePageGUI gui) {
		UUID uuid = player.getUniqueId();

		Integer lastTickOpened = LAST_TICK_OPENED.get(uuid);
		int currentTickOpened = player.getTicksLived();
		if (lastTickOpened != null) {
			if (currentTickOpened - lastTickOpened >= 0 && currentTickOpened - lastTickOpened < TIMEOUT) {
				MessagingUtils.sendActionBarMessage(player, "Please wait one second before trying to open the GUI again.");
				return;
			}
		}

		LAST_TICK_OPENED.put(uuid, currentTickOpened);

		if (!GUI_MAPPINGS.containsKey(uuid)) {
			GUI_MAPPINGS.put(uuid, gui);
			gui.openGUI(GUI_MAPPINGS);
		} else {
			GUI_MAPPINGS.remove(uuid);
			MessagingUtils.sendActionBarMessage(player, "GUI opening canceled. If this problem persists, try relogging.");
		}
	}

	private @Nullable SinglePageGUI getGUI(InventoryInteractEvent event) {
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

	public static void rejectLoad(Player player, String reason) {
		player.closeInventory();
		MessagingUtils.sendActionBarMessage(player, reason);
		if (GUI_MAPPINGS.containsKey(player.getUniqueId())) {
			GUI_MAPPINGS.remove(player.getUniqueId());
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		SinglePageGUI gui = getGUI(event);
		if (gui != null) {
			gui.registerClick(event);
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void inventoryInteractEvent(InventoryInteractEvent event) {
		if (getGUI(event) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void inventoryCloseEvent(InventoryCloseEvent event) {
		UUID uuid = event.getPlayer().getUniqueId();
		SinglePageGUI gui = GUI_MAPPINGS.get(uuid);

		if (gui != null && gui.contains(event.getInventory())) {
			GUI_MAPPINGS.remove(uuid);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		GUI_MAPPINGS.remove(event.getPlayer().getUniqueId());
	}

}
