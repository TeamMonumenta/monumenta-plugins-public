package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public class SpellMusic extends Spell {
	// All times are in ticks
	private final LivingEntity mBoss;
	private final String mTrack;
	private final int mDuration;
	private final int mInterval;
	private final int mDelay;
	private final double mRadiusInner;
	private final double mRadiusOuter;
	private final boolean mClear;
	private final int mClearDelay;

	private final HashMap<Player, Integer> mListeners = new HashMap<>();

	private int mT = 0;

	public SpellMusic(LivingEntity boss, String track, int duration, int interval, int delay, double radiusInner, double radiusOuter, boolean clear, int clearDelay) {
		mBoss = boss;
		mTrack = track;
		mDuration = duration;
		mInterval = interval;
		mDelay = delay;
		mRadiusInner = radiusInner;
		mRadiusOuter = radiusOuter;
		mClear = clear;
		mClearDelay = clearDelay;
	}

	@Override
	public void run() {
		if (mT > mDelay) {
			Location loc = mBoss.getLocation();
			for (Player player : new ArrayList<>(mListeners.keySet())) {
				int duration = mListeners.get(player) - 5;
				if (duration <= 0 || !player.isOnline()) {
					mListeners.remove(player);
					continue;
				}
				if (mClear && player.getLocation().distance(loc) > mRadiusOuter) {
					clear(player);
					continue;
				}
				mListeners.put(player, duration);
			}

			List<Player> players = PlayerUtils.playersInRange(loc, mRadiusInner, true);
			players.removeIf(mListeners::containsKey);
			for (Player player : players) {
				mListeners.put(player, mDuration + mInterval);
				play(player);
			}
		}
		mT += 5;
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		if (mClear) {
			for (Player player : new ArrayList<>(mListeners.keySet())) {
				clear(player);
			}
		}
	}

	private void play(Player player) {
		stop(player);
		PlayerUtils.executeCommandOnPlayer(player, "playsound " + mTrack + " record @s ~ ~ ~ 2");
	}

	private void stop(Player player) {
		// Stops all music to avoid overlapping
		PlayerUtils.executeCommandOnPlayer(player, "stopsound @s record");
	}

	private void clear(Player player) {
		mListeners.remove(player);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> stop(player), mClearDelay);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
