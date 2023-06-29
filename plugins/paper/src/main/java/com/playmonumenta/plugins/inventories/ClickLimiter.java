package com.playmonumenta.plugins.inventories;

import com.playmonumenta.plugins.Plugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * This is just for limiting clicks to 20s
 */
public class ClickLimiter {
	private static final Map<UUID, BukkitRunnable> mCooldowns = new HashMap<>();

	public static boolean isLocked(Player player) {
		return isLocked(player, 5);
	}

	public static boolean isLocked(Player player, long delay) {
		UUID uuid = player.getUniqueId();
		if (mCooldowns.containsKey(uuid)) {
			return true;
		}
		BukkitRunnable runnable = mCooldowns.remove(uuid);
		if (runnable != null) {
			runnable.cancel();
		}
		runnable = new BukkitRunnable() {
			@Override
			public void run() {
				mCooldowns.remove(uuid);
			}
		};
		runnable.runTaskLaterAsynchronously(Plugin.getInstance(), delay);
		mCooldowns.put(uuid, runnable);
		return false;
	}

	public static boolean removeLock(Player player) {
		UUID uuid = player.getUniqueId();
		if (mCooldowns.containsKey(uuid)) {
			mCooldowns.get(uuid).cancel();
			mCooldowns.remove(uuid);
			return true;
		}
		return false;
	}
}
