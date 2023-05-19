package com.playmonumenta.plugins.bosses;

import com.playmonumenta.plugins.Plugin;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class ChargeUpManager {
	private int mTime;
	private int mChargeTime;
	private Component mTitle;
	private final @Nullable Entity mBoss;
	private Location mLoc;
	private final boolean mRequireBoss; //If this is true then boss bar is hidden if mBoss = null;
	private @Nullable BukkitRunnable mRunnable;
	private int mRefresh;

	private final int mRange;
	private final BossBar mBar;

	public ChargeUpManager(Entity boss, int chargeTime, Component title, BossBar.Color color, BossBar.Overlay style, int range) {
		this(boss.getLocation(), boss, chargeTime, title, color, style, range);
	}

	public ChargeUpManager(Location loc, @Nullable Entity boss, int chargeTime, Component title, BossBar.Color color, BossBar.Overlay style, int range) {
		mLoc = loc;
		mChargeTime = chargeTime;
		mBoss = boss;
		mRequireBoss = boss != null;
		mTitle = title;
		mTime = 0;
		mRange = range;
		mBar = BossBar.bossBar(mTitle, 0, color, style);
		mBar.progress(0);
		mRefresh = 0;
		mRunnable = null;
	}

	public void reset() {
		mBar.progress(0);
		mLoc.getWorld().hideBossBar(mBar);
		mTime = 0;
		mRunnable = null;
	}


	public boolean nextTick() {
		return nextTick(1);
	}

	public boolean nextTick(int time) {
		mTime += time;
		update();
		return mTime >= mChargeTime;
	}

	public boolean previousTick() {
		return previousTick(1);
	}

	public boolean previousTick(int time) {
		mTime -= time;
		update();
		return mTime <= 0;
	}

	public void update() {
		if (mBoss != null) {
			mLoc = mBoss.getLocation();
		}

		mRefresh = 0;
		if (mRunnable == null) {
			mRunnable = new BukkitRunnable() {
				@Override
				public void run() {
					mRefresh++;

					if (mRefresh >= 20) {
						this.cancel();
						mLoc.getWorld().hideBossBar(mBar);
						mRefresh = 0;
						mRunnable = null;
					}
				}
			};

			mRunnable.runTaskTimer(Plugin.getInstance(), 0, 1);
		}

		for (Player player : mLoc.getWorld().getPlayers()) {
			if ((!mRequireBoss || (mBoss != null && mBoss.isValid())) && player.getLocation().distance(mLoc) < mRange) {
				player.showBossBar(mBar);
			} else {
				player.hideBossBar(mBar);
			}
		}

		float progress = (float) mTime / (float) mChargeTime;
		if (progress > 1) {
			progress = 1;
		} else if (progress < 0) {
			progress = 0;
		}
		mBar.progress(progress);
	}

	public void setProgress(double progress) {
		setProgress((float) progress);
	}

	public void setProgress(float progress) {
		if (progress > 1) {
			progress = 1;
		} else if (progress < 0) {
			progress = 0;
		}
		mBar.progress(progress);
		mRefresh = 0;
	}

	public void setTime(int time) {
		mTime = time;
	}

	public int getTime() {
		return mTime;
	}

	public void setChargeTime(int chargeTime) {
		mChargeTime = chargeTime;
	}

	public int getChargeTime() {
		return mChargeTime;
	}

	public void setTitle(Component title) {
		mBar.name(title);
		mTitle = title;
	}

	public Component getTitle() {
		return mBar.name();
	}

	public void setColor(BossBar.Color color) {
		mBar.color(color);
	}

	public BossBar.Color getColor() {
		return mBar.color();
	}

	public void setOverlay(BossBar.Overlay overlay) {
		mBar.overlay(overlay);
	}

	public BossBar.Overlay getOverlay() {
		return mBar.overlay();
	}

	public void remove() {
		mLoc.getWorld().hideBossBar(mBar);
	}
}
