package com.playmonumenta.plugins.player.activity;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ActivityManager {
	private static @Nullable ActivityManager mManager = null;
	private final Map<UUID, Double> mDamageDealt = new HashMap<>();
	private final Map<UUID, Double> mHealingDealt = new HashMap<>();
	public final Map<UUID, Integer> mActivity = new HashMap<>();
	public static final int THRESHOLD_R1 = 100;
	public static final int THRESHOLD_R2 = 200;
	public static final int THRESHOLD_R3 = 300;
	private final Plugin mPlugin;

	public ActivityManager(Plugin plugin) {
		mManager = this;
		mPlugin = plugin;
	}

	public static ActivityManager getManager() {
		return mManager;
	}

	public void clearPlayerStats(Player player) {
		UUID playerId = player.getUniqueId();
		mDamageDealt.put(playerId, 0.0);
		mHealingDealt.put(playerId, 0.0);
		mActivity.put(playerId, 0);
	}

	public boolean isActive(Player player) {
		return mActivity.getOrDefault(player.getUniqueId(), 0) > 0;
	}

	public void addDamageDealt(Player player, Double damage) {
		UUID playerId = player.getUniqueId();
		double updatedDamage = mDamageDealt.getOrDefault(playerId, 0.0) + damage;
		mDamageDealt.put(playerId, updatedDamage);
		if (ServerProperties.getAbilityEnhancementsEnabled()) {
			if (updatedDamage > THRESHOLD_R3) {
				mDamageDealt.put(playerId, updatedDamage - THRESHOLD_R3);
				addActivity(player);
			}
		} else
		if (ServerProperties.getClassSpecializationsEnabled()) {
			if (updatedDamage > THRESHOLD_R2) {
				mDamageDealt.put(playerId, updatedDamage - THRESHOLD_R2);
				addActivity(player);
			}
		} else if (updatedDamage > THRESHOLD_R1) {
			mDamageDealt.put(playerId, updatedDamage - THRESHOLD_R1);
			addActivity(player);
		}
	}

	public void addHealingDealt(Player player, Double healing) {
		UUID playerId = player.getUniqueId();
		double updatedHealing = mHealingDealt.getOrDefault(playerId, 0.0) + healing;
		double damageDealt = mDamageDealt.getOrDefault(playerId, 0.0);
		mHealingDealt.put(playerId, updatedHealing);
		if (ServerProperties.getAbilityEnhancementsEnabled()) {
			if (updatedHealing > THRESHOLD_R3 / 4 && damageDealt > 1) {
				mHealingDealt.put(playerId, updatedHealing - THRESHOLD_R3 / 4);
				mDamageDealt.put(playerId, 0.0);
				addActivity(player);
			}
		} else
		if (ServerProperties.getClassSpecializationsEnabled()) {
			if (updatedHealing > THRESHOLD_R2 / 4 && damageDealt > 1) {
				mHealingDealt.put(playerId, updatedHealing - THRESHOLD_R2 / 4);
				mDamageDealt.put(playerId, 0.0);
				addActivity(player);
			}
		} else if (updatedHealing > THRESHOLD_R1 / 4 && damageDealt > 1) {
			mHealingDealt.put(playerId, updatedHealing - THRESHOLD_R1 / 4);
			mDamageDealt.put(playerId, 0.0);
			addActivity(player);
		}
	}

	public void addActivity(Player player) {
		mActivity.put(player.getUniqueId(), mActivity.getOrDefault(player.getUniqueId(), 0) + 1);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mActivity.put(player.getUniqueId(), Math.max(mActivity.getOrDefault(player.getUniqueId(), 0) - 1, 0));
			MMLog.fine("Player " + player.getName() + " has activity: " + mActivity.getOrDefault(player.getUniqueId(), 0));
		}, 20 * 6 * 60);
		MMLog.fine("Player " + player.getName() + " has activity: " + mActivity.getOrDefault(player.getUniqueId(), 0));
	}
}
