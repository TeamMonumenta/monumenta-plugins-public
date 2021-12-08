package com.playmonumenta.plugins.integrations;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

import de.jeff_media.chestsort.api.ChestSortAPI;
import de.jeff_media.chestsort.api.ChestSortEvent;

public class ChestSortIntegration implements Listener {
	private static boolean checkedForPlugin = false;
	private static boolean mIsEnabled = false;
	private final com.playmonumenta.plugins.Plugin mPlugin;
	private final Set<UUID> mClicked = new HashSet<>();

	public ChestSortIntegration(com.playmonumenta.plugins.Plugin plugin) {
		mPlugin = plugin;
		plugin.getLogger().info("Enabling ChestSort integration");
	}

	private static void checkForPlugin() {
		mIsEnabled = Bukkit.getServer().getPluginManager().isPluginEnabled("ChestSort");
		checkedForPlugin = true;
	}

	public static boolean isPresent() {
		if (!checkedForPlugin) {
			checkForPlugin();
		}

		return mIsEnabled;
	}

	public static void sortInventory(Inventory inventory) {
		if (!isPresent()) {
			return;
		}

		if (inventory instanceof PlayerInventory) {
			ChestSortAPI.sortInventory(inventory, 9, 35);
		} else {
			ChestSortAPI.sortInventory(inventory);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (!isPresent()) {
			return;
		}
		if (event.getWhoClicked() instanceof Player) {
			Player player = (Player) event.getWhoClicked();
			InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, player, event);
			Inventory inventory = event.getClickedInventory();
			if (inventory == null) {
				return;
			}

			if (!(inventory instanceof PlayerInventory) && ZoneUtils.hasZoneProperty(player, ZoneProperty.SHOPS_POSSIBLE)) {
				/* Don't sort market chests */
				return;
			}

			if (event.getClick() != null
				&& event.getClick().equals(ClickType.RIGHT)
				&& inventory.getItem(event.getSlot()) == null
				&& event.getAction().equals(InventoryAction.NOTHING)
				&& event.getSlotType() != InventoryType.SlotType.CRAFTING) {

				// Player right clicked a non-crafting empty space and nothing happened
				// Check if the last thing the player did was also the same thing.
				// If so, sort the chest
				if (mClicked.contains(player.getUniqueId())) {
					ChestSortIntegration.sortInventory(inventory);
					player.updateInventory();
					mClicked.remove(player.getUniqueId());

					// Just in case we sorted an item on top of where the player was clicking
					event.setCancelled(true);
				} else {
					// Mark the player as having right clicked an empty slot
					mClicked.add(player.getUniqueId());
				}
			} else {
				// Player did something else with this inventory - clear the marker
				mClicked.remove(player.getUniqueId());
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void inventoryOpenEvent(InventoryOpenEvent event) {
		if (!isPresent()) {
			return;
		}

		if (event.getPlayer() instanceof Player) {
			mClicked.remove(event.getPlayer().getUniqueId());
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void inventoryCloseEvent(InventoryCloseEvent event) {
		if (!isPresent()) {
			return;
		}

		InventoryHolder holder = event.getInventory().getHolder();
		if (holder != null && holder instanceof Player) {
			mClicked.remove(((Player) holder).getUniqueId());
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void chestSortEvent(ChestSortEvent event) {
		for (Map.Entry<ItemStack, Map<String, String>> itemSortMapPair : event.getSortableMaps().entrySet()) {
			ItemStack item = itemSortMapPair.getKey();
			Map<String, String> sortMap = itemSortMapPair.getValue();

			// Fix name sorting to ignore formatting
			String customName = sortMap.get("{customName}");
			if (customName != null) {
				customName = ChatColor.stripColor(customName);
				sortMap.put("{customName}", customName);
			}

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
			} else {
				strBookTitle = ChatColor.stripColor(strBookTitle);
			}

			String strBookAuthor = ItemUtils.getBookAuthor(item);
			if (strBookAuthor == null) {
				strBookAuthor = "~bookAuthor~";
			} else {
				strBookAuthor = ChatColor.stripColor(strBookAuthor);
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
