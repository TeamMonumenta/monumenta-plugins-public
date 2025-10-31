package com.playmonumenta.plugins.inventories;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.MetadataUtils;
import org.bukkit.entity.Player;

/**
 * This is just for limiting clicks to 4 times a second
 */
public class ClickLimiter {
	private static final String DEFAULT_METADATA_KEY = "InventoryClickLimiter";

	public static boolean isLocked(Player player) {
		return isLocked(player, 5);
	}

	public static boolean isLocked(Player player, int delay) {
		return isLocked(player, 5, DEFAULT_METADATA_KEY);
	}

	public static boolean isLocked(Player player, int delay, String key) {
		return !MetadataUtils.checkOnceInRecentTicks(Plugin.getInstance(), player, key, delay);
	}

	public static void removeLock(Player player) {
		removeLock(player, DEFAULT_METADATA_KEY);
	}

	public static void removeLock(Player player, String key) {
		MetadataUtils.removeMetadata(player, key);
	}
}
