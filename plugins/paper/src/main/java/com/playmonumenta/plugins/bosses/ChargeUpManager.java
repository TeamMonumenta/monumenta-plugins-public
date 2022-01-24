package com.playmonumenta.plugins.bosses;

import com.playmonumenta.scriptedquests.Plugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import javax.annotation.Nullable;

public class ChargeUpManager {

	private int mTime;
	private int mChargeTime;
	private String mTitle;
	private @Nullable LivingEntity mBoss;
	private Location mLoc;
	private final int mRange;
	private final BossBar mBar;
	private @Nullable BukkitRunnable mRunnable;
	private int mRefresh;

	public ChargeUpManager(LivingEntity boss, int chargeTime, String title, BarColor color, BarStyle style, int range) {
		this(boss.getLocation(), boss, chargeTime, title, color, style, range);
	}

	public ChargeUpManager(Location loc, @Nullable LivingEntity boss, int chargeTime, String title, BarColor color, BarStyle style, int range) {
		mLoc = loc;
		mChargeTime = chargeTime;
		mBoss = boss;
		mTitle = title;
		mTime = 0;
		mRange = range;
		mBar = Bukkit.getServer().createBossBar(title, color, style);
		mBar.setVisible(false);
		mBar.setProgress(0);
		mRefresh = 0;
		mRunnable = null;
	}

	public void setChargeTime(int chargeTime) {
		mChargeTime = chargeTime;
	}

	public void reset() {
		mBar.setProgress(0);
		mBar.setVisible(false);
		mTime = 0;
		mRunnable = null;
	}

	public int getTime() {
		return mTime;
	}

	public void setTime(int time) {
		mTime = time;
	}

	public boolean nextTick() {
		return nextTick(1);
	}

	public boolean nextTick(int time) {
		mTime += time;
		update();
		return mTime >= mChargeTime;
	}

	public void setTitle(String title) {
		mBar.setTitle(title);
		mTitle = title;
	}

	public void setColor(BarColor color) {
		mBar.setColor(color);
	}

	public BarColor getColor() {
		return mBar.getColor();
	}

	public void update() {

		if (!mBar.isVisible()) {
			mBar.setVisible(true);
			mBar.setTitle(mTitle);
		}
		if (mBoss != null) {
			mLoc = mBoss.getLocation();
			if (mBoss.getHealth() <= 0 || mBoss.isDead() || !mBoss.isValid()) {
				mBar.setVisible(false);

			}
		}

		mRefresh = 0;
		if (mRunnable == null) {
			mRunnable = new BukkitRunnable() {

				@Override
				public void run() {
					mRefresh++;

					if (mRefresh >= 20 * 1) {
						this.cancel();
						mBar.setVisible(false);
						mRefresh = 0;
						mRunnable = null;
					}
				}

			};

			mRunnable.runTaskTimer(Plugin.getInstance(), 0, 1);
		}

		for (Player player : Bukkit.getOnlinePlayers()) {
			if (player.getWorld().equals(mLoc.getWorld()) && player.getLocation().distance(mLoc) < mRange) {
				mBar.addPlayer(player);
			} else {
				mBar.removePlayer(player);
			}
		}

		double progress = (double) mTime / (double) mChargeTime;
		if (progress > 1) {
			progress = 1;
		} else if (progress < 0) {
			progress = 0;
		}
		mBar.setProgress(progress);
	}

	public void setProgress(double progress) {
		if (progress > 1) {
			progress = 1;
		} else if (progress < 0) {
			progress = 0;
		}
		mBar.setProgress(progress);
		mRefresh = 0;
	}

	public void remove() {
		mBar.setVisible(false);
	}

}

