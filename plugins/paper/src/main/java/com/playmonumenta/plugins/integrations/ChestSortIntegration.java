package com.playmonumenta.plugins.integrations;

import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.utils.ItemUtils;

import de.jeff_media.ChestSort.ChestSortPlugin;
import de.jeff_media.ChestSortAPI.ChestSortAPI;
import de.jeff_media.ChestSortAPI.ChestSortEvent;

public class ChestSortIntegration implements Listener {
	private static boolean checkedForPlugin = false;
	private static ChestSortAPI chestSortAPI = null;

	public ChestSortIntegration(Logger logger) {
		logger.info("Enabling ChestSort integration");
	}

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

	@EventHandler(priority = EventPriority.HIGH)
	public void chestSortEvent(ChestSortEvent event) {
		for (Map.Entry<ItemStack, Map<String, String>> itemSortMapPair : event.getSortableMaps().entrySet()) {
			ItemStack item = itemSortMapPair.getKey();
			Map<String, String> sortMap = itemSortMapPair.getValue();

			int itemCount = 0;
			if (item != null) {
				itemCount = item.getAmount();
			}
			// Rather than sorting by the count from lowest to highest, sort from highest to lowest.
			// A max custom stack has 127 items, a min custom stack has -128. Scale from max -> 0 to min -> 255.
			String strCount = String.format("%03d", 127 - itemCount);

			String strRegion = "~region~"; // Missing values start with ~ and wind up at the end
			ItemUtils.ItemRegion region = ItemUtils.getItemRegion(item);
			if (region != null) {
				int ordinal = region.ordinal();
				String name = region.toString();
				strRegion = Integer.toString(ordinal) + "_" + name;
			}

			String strTier = "~tier~";
			ItemUtils.ItemTier tier = ItemUtils.getItemTier(item);
			if (tier != null) {
				int ordinal = tier.ordinal();
				String name = tier.toString();
				strTier = String.format("%02d_%s", ordinal, name);
			}

			String strQuest = ItemUtils.getItemQuestId(item);
			if (strQuest == null) {
				strQuest = "~quest~";
			}

			String strBookTitle = ItemUtils.getBookTitle(item);
			if (strBookTitle == null) {
				strBookTitle = "~bookTitle~";
			}

			String strBookAuthor = ItemUtils.getBookAuthor(item);
			if (strBookAuthor == null) {
				strBookAuthor = "~bookAuthor~";
			}

			sortMap.put("{count}", strCount);
			sortMap.put("{region}", strRegion);
			sortMap.put("{tier}", strTier);
			sortMap.put("{quest}", strQuest);
			sortMap.put("{bookTitle}", strBookTitle);
			sortMap.put("{bookAuthor}", strBookAuthor);
		}
	}
}
