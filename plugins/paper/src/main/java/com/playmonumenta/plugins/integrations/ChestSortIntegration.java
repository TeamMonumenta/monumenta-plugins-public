package com.playmonumenta.plugins.integrations;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import de.jeff_media.ChestSort.ChestSortAPI;
import de.jeff_media.ChestSort.ChestSortPlugin;

public class ChestSortIntegration {
	private static boolean checkedForPlugin = false;
	private static ChestSortAPI chestSortAPI = null;

	private static void checkForPlugin() {
		Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("ChestSort");
		if (plugin instanceof ChestSortPlugin) {
			chestSortAPI = ((ChestSortPlugin)plugin).getAPI();
		}
		checkedForPlugin = true;
	}

	public static boolean isPresent() {
		if (!checkedForPlugin) {
			checkForPlugin();
		}

		return chestSortAPI != null;
	}

	public static void sortInventory(Inventory inventory) {
		if (isPresent()) {
			if (inventory instanceof PlayerInventory) {
				chestSortAPI.sortInventory(inventory, 9, 35);
			} else {
				chestSortAPI.sortInventory(inventory);
			}
		}
	}
}
