package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.Plugin;
import java.util.OptionalInt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

public class TOVUtils {

	public static final String UNOPENED_CACHE_NAME = "TOVCache";
	public static final String OPENED_CACHE_NAME = "Cache";
	public static final String CACHE_LOOT_TABLE = "r2/treasure_hunt/cache";

	public static final String CACHES_OPENED_SCORE = "TreasureHunt";
	public static final String DAILY_CACHES_OPENED_SCORE = "DailyLimitTOV";
	public static final String CACHE_COUNTER_SCORE = "TOVGlobal";
	public static final String COUNTER_NAME = "$Counter";

	public static boolean canBreak(Plugin plugin, Player player, Block block, BlockBreakEvent event) {
		BlockState blockState = block.getState();
		if (blockState instanceof Chest) {
			String name = ((Chest) blockState).getCustomName();
			if (UNOPENED_CACHE_NAME.equals(name) || OPENED_CACHE_NAME.equals(name)) {
				MessagingUtils.sendActionBarMessage(player, "You cannot break Treasures of Viridia Caches.");

				return false;
			}
		}

		return true;
	}

	public static boolean isUnopenedTovLootCache(Block block) {
		return block.getState() instanceof Chest chest && UNOPENED_CACHE_NAME.equals(chest.getCustomName());
	}

	public static boolean isOpenedTovLootCache(Block block) {
		return block.getState() instanceof Chest chest && OPENED_CACHE_NAME.equals(chest.getCustomName());
	}

	public static boolean setTOVLootTable(Plugin plugin, Player player, Block block) {
		if (block.getState() instanceof Chest chest && UNOPENED_CACHE_NAME.equals(chest.getCustomName())) {
			if (!canOpen(plugin, player)) {
				return false;
			}

			chest.setCustomName(OPENED_CACHE_NAME);
			chest.setLootTable(Bukkit.getLootTable(NamespacedKeyUtils.fromString("epic:" + CACHE_LOOT_TABLE)));
			chest.update();
			return true;
		}
		return false;
	}

	private static boolean canOpen(Plugin plugin, Player player) {
		// can always open caches if less than 100 caches claimed in total
		if (ScoreboardUtils.getScoreboardValue(player, CACHES_OPENED_SCORE).orElse(0) < 100) {
			return true;
		}

		OptionalInt counter = ScoreboardUtils.getScoreboardValue(COUNTER_NAME, CACHE_COUNTER_SCORE);
		if (counter.isPresent()) {
			// can only open one cache per spawn
			int cycleCounter = counter.getAsInt();
			if (cycleCounter == ScoreboardUtils.getScoreboardValue(player, CACHE_COUNTER_SCORE).orElse(0)) {
				MessagingUtils.sendActionBarMessage(player, "You cannot open more than 1 cache per spawn.");
				Bukkit.getScheduler().runTaskLater(plugin, () -> player.closeInventory(), 1);
				return false;
			}
			// cannot open more than 4 caches a day
			int remainingCaches = ScoreboardUtils.getScoreboardValue(player, DAILY_CACHES_OPENED_SCORE).orElse(4);
			if (remainingCaches <= 0) {
				MessagingUtils.sendActionBarMessage(player, "You have reached your daily limit of caches claimed.");
				Bukkit.getScheduler().runTaskLater(plugin, () -> player.closeInventory(), 1);
				return false;
			} else if (remainingCaches == 1) {
				player.sendMessage(Component.text("You have now reached your daily limit of caches claimed.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			}

			ScoreboardUtils.setScoreboardValue(player, CACHE_COUNTER_SCORE, cycleCounter);
			ScoreboardUtils.setScoreboardValue(player, DAILY_CACHES_OPENED_SCORE, remainingCaches - 1);
			return true;
		}

		return false;
	}

}
