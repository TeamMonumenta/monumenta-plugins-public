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

	public static final String CACHE_NAME = "TOVCache";
	public static final String CACHE_LOOT_TABLE = "r2/treasure_hunt/cache";

	public static final String CACHES_OPENED_SCORE = "TreasureHunt";
	public static final String CACHE_COUNTER_SCORE = "TOVGlobal";
	public static final String COUNTER_NAME = "$Counter";

	public static boolean canBreak(Plugin plugin, Player player, Block block, BlockBreakEvent event) {
		BlockState blockState = block.getState();
		if (blockState instanceof Chest && CACHE_NAME.equals(((Chest) blockState).getCustomName())) {
			MessagingUtils.sendActionBarMessage(plugin, player, "You cannot break Treasures of Viridia Caches.");
		}

		return true;
	}

	public static void setTOVLootTable(Plugin plugin, Player player, Block block) {
		BlockState blockState = block.getState();
		if (blockState instanceof Chest) {
			Chest chest = (Chest) blockState;
			if (CACHE_NAME.equals(chest.getCustomName()) && canOpen(plugin, player)) {
				chest.setCustomName(null);
				chest.setLootTable(Bukkit.getLootTable(new NamespacedKey("epic", CACHE_LOOT_TABLE)));
				chest.update();
			}
		}
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
