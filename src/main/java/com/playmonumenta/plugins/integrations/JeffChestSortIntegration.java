package com.playmonumenta.plugins.integrations;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import de.jeffclan.JeffChestSort.JeffChestSortPlugin;

public class JeffChestSortIntegration {
	private static boolean checkedForPlugin = false;
	private static JeffChestSortPlugin chestSortPlugin = null;

	private static void checkForPlugin() {
		Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("ChestSort");
		if (plugin instanceof JeffChestSortPlugin) {
			chestSortPlugin = (JeffChestSortPlugin)plugin;
		}
		checkedForPlugin = true;
	}

	public static boolean isPresent() {
		if (!checkedForPlugin) {
			checkForPlugin();
		}

		return chestSortPlugin != null;
	}

	public static void sortInventory(Inventory inventory) {
		if (isPresent()) {
			chestSortPlugin.sortInventory(inventory);
		}
	}
}
