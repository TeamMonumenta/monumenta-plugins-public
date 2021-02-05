package com.playmonumenta.plugins.utils;

import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;

public class TOVUtils {

	public static final String UNOPENED_CACHE_NAME = "TOVCache";
	public static final String OPENED_CACHE_NAME = "Cache";
	public static final String CACHE_LOOT_TABLE = "r2/treasure_hunt/cache";

	public static final String CACHES_OPENED_SCORE = "TreasureHunt";
	public static final String CACHE_COUNTER_SCORE = "TOVGlobal";
	public static final String COUNTER_NAME = "$Counter";

	public static boolean canBreak(Plugin plugin, Player player, Block block, BlockBreakEvent event) {
		BlockState blockState = block.getState();
		if (blockState instanceof Chest) {
			String name = ((Chest) blockState).getCustomName();
			if (UNOPENED_CACHE_NAME.equals(name) || OPENED_CACHE_NAME.equals(name)) {
				MessagingUtils.sendActionBarMessage(plugin, player, "You cannot break Treasures of Viridia Caches.");

				return false;
			}
		}

		return true;
	}

	public static boolean setTOVLootTable(Plugin plugin, Player player, Block block) {
		BlockState blockState = block.getState();
		if (blockState instanceof Chest) {
			Chest chest = (Chest) blockState;
			String name = ((Chest) blockState).getCustomName();
			if (UNOPENED_CACHE_NAME.equals(name) || OPENED_CACHE_NAME.equals(name)) {
				if (!canOpen(plugin, player)) {
					return false;
				}

				if (UNOPENED_CACHE_NAME.equals(name)) {
					chest.setCustomName(OPENED_CACHE_NAME);
					chest.setLootTable(Bukkit.getLootTable(new NamespacedKey("epic", CACHE_LOOT_TABLE)));
					chest.update();
				}
			}
		}

		return true;
	}

	private static boolean canOpen(Plugin plugin, Player player) {
		if (ScoreboardUtils.getScoreboardValue(player, CACHES_OPENED_SCORE) < 100) {
			return true;
		}

		Optional<Integer> counter = ScoreboardUtils.getScoreboardValue(COUNTER_NAME, CACHE_COUNTER_SCORE);
		if (counter.isPresent()) {
			int value = counter.get();
			if (value == ScoreboardUtils.getScoreboardValue(player, CACHE_COUNTER_SCORE)) {
				MessagingUtils.sendActionBarMessage(plugin, player, "You cannot open more than 1 cache per spawn.");
				new BukkitRunnable() {
					@Override
					public void run() {
						player.closeInventory();
					}
				}.runTaskLater(plugin, 1);
				return false;
			} else {
				ScoreboardUtils.setScoreboardValue(player, CACHE_COUNTER_SCORE, value);
				return true;
			}
		}

		return false;
	}

}
