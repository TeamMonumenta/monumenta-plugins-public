package com.playmonumenta.plugins.cooking;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class CookingTableInventoryManager {

	private static final String ERROR_COOKING_RATE_LIMITED = ChatColor.RED + "Too fast! Please try again";

	private final Plugin mPlugin;
	private final HashSet<UUID> mRateLimited = new HashSet<>();
	private final HashMap<UUID, CookingTableInventory> mInventories = new HashMap<>();

	public CookingTableInventoryManager(Plugin plugin) {
		mPlugin = plugin;
	}

	void openTable(Player player) {
		// limit fast openings
		if (mRateLimited.contains(player.getUniqueId())) {
			player.sendMessage(ERROR_COOKING_RATE_LIMITED);
			return;
		}
		mRateLimited.add(player.getUniqueId());
		new BukkitRunnable() {
			@Override
			public void run() {
				mRateLimited.remove(player.getUniqueId());
			}
		}.runTaskLater(mPlugin, 10);

		player.closeInventory(InventoryCloseEvent.Reason.OPEN_NEW);
		CookingTableInventory cookingTableInventory = new CookingTableInventory(mPlugin, player);
		mInventories.put(player.getUniqueId(), cookingTableInventory);
		cookingTableInventory.openTable();
	}

	void closeTable(UUID uuid) {
		if (mInventories.containsKey(uuid)) {
			CookingTableInventory inv = mInventories.remove(uuid);
			inv.closeTable();
		}
	}

	boolean isPlayerCookingHUD(Player player, Inventory inv) {
		CookingTableInventory i = mInventories.get(player.getUniqueId());
		return i != null && i.getInventory() == inv;
	}

	void updateTable(Player player) {
		mPlugin.getServer().getScheduler().scheduleSyncDelayedTask(mPlugin, () -> mInventories.get(player.getUniqueId()).updateTable(), 0L);
	}

	void cookAll(Player player) {
		mInventories.get(player.getUniqueId()).cookAll();
	}

	void cookOne(Player player) {
		mInventories.get(player.getUniqueId()).cookOne();
	}
}
