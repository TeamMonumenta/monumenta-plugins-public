package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.scriptedquests.managers.SongManager;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

public class SpellMusic extends Spell {
	// All times are in ticks
	private final LivingEntity mBoss;
	private final String mTrack;
	private final int mDuration;
	private final float mVolume;
	private final int mDelay;
	private final double mRadiusInner;
	private final double mRadiusOuter;
	private final boolean mClear;
	private final int mClearDelay;
	private final boolean mForce;

	private final List<Player> mListeners = new ArrayList<>();

	private int mT = 0;

	public SpellMusic(LivingEntity boss, String track, int duration, float volume, int delay, double radiusInner, double radiusOuter, boolean clear, int clearDelay, boolean force) {
		mBoss = boss;
		mTrack = track;
		mDuration = duration;
		mVolume = volume;
		mDelay = delay;
		mRadiusInner = radiusInner;
		mRadiusOuter = radiusOuter;
		mClear = clear;
		mClearDelay = clearDelay;
		mForce = force;
	}

	@Override
	public void run() {
		if (mT > mDelay) {
			Location loc = mBoss.getLocation();
			PlayerUtils.playersInRange(loc, mRadiusInner, true).stream()
				.filter(p -> !mListeners.contains(p))
				.toList()
				.forEach(p -> {
					play(p);
					mListeners.add(p);
				});

			if (mClear) {
				mListeners.stream()
					.filter(p -> p.getLocation().distanceSquared(loc) > mRadiusOuter * mRadiusOuter)
					.toList()
					.forEach(p -> {
						clear(p);
						mListeners.remove(p);
					});
			}
		}
		mT += 5;
	}

	@Override
	public void onDeath(@Nullable EntityDeathEvent event) {
		if (mClear) {
			mListeners.forEach(this::clear);
			mListeners.clear();
		} else {
			mListeners.forEach(p -> SongManager.stopSong(p, false));
		}
	}

	private SongManager.Song getSong() {
		return new SongManager.Song(mTrack, SoundCategory.RECORDS, mDuration / 20.0, true, mVolume, 1);
	}

	private void play(Player player) {
		SongManager.playSong(player, getSong(), mForce);
	}

	private void stop(Player player) {
		SongManager.Song current = SongManager.getCurrentSong(player);
		if (current != null && current.mSongPath.equals(mTrack)) {
			SongManager.stopSong(player, true);
		}
	}

	private void clear(Player player) {
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> stop(player), mClearDelay);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
