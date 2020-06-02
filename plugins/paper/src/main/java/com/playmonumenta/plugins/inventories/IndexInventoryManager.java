package com.playmonumenta.plugins.inventories;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class IndexInventoryManager {

	private static final String ERROR_INDEX_RATE_LIMITED = ChatColor.RED + "Too fast! Please try again";

	private final HashSet<UUID> mRateLimited = new HashSet<>();
	private final HashMap<UUID, IndexInventory> mInventories = new HashMap<>();

	public void openIndex(Player player) {
		// limit fast openings
		if (mRateLimited.contains(player.getUniqueId())) {
			player.sendMessage(ERROR_INDEX_RATE_LIMITED);
			return;
		}
		mRateLimited.add(player.getUniqueId());
		new BukkitRunnable() {
			@Override
			public void run() {
				mRateLimited.remove(player.getUniqueId());
			}
		}.runTaskLater(Plugin.getInstance(), 10);

		player.closeInventory(InventoryCloseEvent.Reason.OPEN_NEW);
		IndexInventory indexInventory = new IndexInventory(player);
		mInventories.put(player.getUniqueId(), indexInventory);
		indexInventory.openTable();
	}

	public boolean isPlayersIndexInventory(Player player, Inventory clickedInventory) {
		IndexInventory i = mInventories.get(player.getUniqueId());
		return i != null && i.getInventory() == clickedInventory;
	}

	void closeIndex(UUID uuid) {
		mInventories.remove(uuid);
	}

	public void pageUp(Player player) {
		mInventories.get(player.getUniqueId()).pageUp();
	}

	public void pageDown(Player player) {
		mInventories.get(player.getUniqueId()).pageDown();
	}

	public void resetFilters(Player player) {
		mInventories.get(player.getUniqueId()).resetFilters();
	}

	public void pickItem(Player player, int slot) {
		mInventories.get(player.getUniqueId()).pickItem(slot);
	}
}
