package com.playmonumenta.plugins.bosses;

import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class BossBarManager {
	@FunctionalInterface
	public interface BossHealthAction {
		void run(LivingEntity boss);
	}

	private final LivingEntity mBoss;
	private final int mRange;
	private final @Nullable Map<Integer, BossHealthAction> mEvents;
	private final BossBar mBar;
	private int mEventCursor;
	private final boolean mCapDamage;

	public BossBarManager(LivingEntity boss, int range, BarColor color, BarStyle style, @Nullable Map<Integer, BossHealthAction> events) {
		this(boss, range, color, style, events, true);
	}

	public BossBarManager(LivingEntity boss, int range, BarColor color, BarStyle style, @Nullable Map<Integer, BossHealthAction> events, boolean bossFog) {
		this(boss, range, color, style, events, bossFog, true);
	}

	public BossBarManager(LivingEntity boss, int range, BarColor color, BarStyle style, @Nullable Map<Integer, BossHealthAction> events, boolean bossFog, boolean capDamage) {
		mBoss = boss;
		mRange = range;
		mEvents = events;
		mEventCursor = 100;
		mCapDamage = capDamage;
		double progress = mBoss.getHealth() / EntityUtils.getMaxHealth(mBoss);
		while (mEvents != null && mEventCursor > (progress * 100)) {
			mEventCursor--;
		}

		mBar = Bukkit.getServer().createBossBar(mBoss.getName(), color, style, BarFlag.PLAY_BOSS_MUSIC);
		if (bossFog) {
			mBar.addFlag(BarFlag.CREATE_FOG);
			mBar.addFlag(BarFlag.DARKEN_SKY);
		}
		mBar.setVisible(true);

		for (Player player : mBoss.getWorld().getPlayers()) {
			if (player.getLocation().distance(mBoss.getLocation()) < mRange) {
				mBar.addPlayer(player);
			}
		}
	}

	public void update() {
		if (mBoss.getHealth() <= 0) {
			mBar.setVisible(false);
		}

		for (Player player : mBoss.getWorld().getPlayers()) {
			if (player.getLocation().distance(mBoss.getLocation()) < mRange) {
				mBar.addPlayer(player);
			} else {
				mBar.removePlayer(player);
			}
		}

		double maxHealth = EntityUtils.getMaxHealth(mBoss);
		double progress = mBoss.getHealth() / maxHealth;
		if (mEvents != null) {
			if (progress * 100 > 99 && mEventCursor >= 99) {
				BossHealthAction event = mEvents.get(mEventCursor);
				if (event != null) {
					MMLog.fine("Running BossHealthAction for " + MessagingUtils.plainText(mBoss.name()) + " at " + mEventCursor + "% health.");
					event.run(mBoss);
					if (mCapDamage) {
						double cap = mEventCursor / 100.0;
						mBoss.setHealth(maxHealth * cap);
						mBar.setProgress(cap);
					}
				}
				mEventCursor--;
			}
			while (true) {
				progress = mBoss.getHealth() / maxHealth;
				if (mEventCursor <= progress * 100) {
					break;
				}
				BossHealthAction event = mEvents.get(mEventCursor);
				if (event != null) {
					MMLog.fine("Running BossHealthAction for " + MessagingUtils.plainText(mBoss.name()) + " at " + mEventCursor + "% health.");
					event.run(mBoss);
					if (mCapDamage) {
						double cap = mEventCursor / 100.0;
						mBoss.setHealth(maxHealth * cap);
						mBar.setProgress(cap);
					}
				}
				mEventCursor--;
			}
		}

		if (!Double.isFinite(progress) || progress > 1.0f || progress < 0f) {
			MMLog.warning("Boss '" + mBoss.getName() + "' has invalid health " +
				mBoss.getHealth() + " out of max " + maxHealth);
		} else {
			mBar.setProgress(progress);
		}
	}

	public void setTitle(String newTitle) {
		mBar.setTitle(newTitle);
	}

	public void setColor(BarColor barColor) {
		mBar.setColor(barColor);
	}

	public void remove() {
		mBar.setVisible(false);
	}

	public boolean capsDamage() {
		return mCapDamage;
	}

	// Returns the highest health percentage (0 through 100) lower than current bar progress with an unused BossHealthAction
	// If none exist, returns 0
	public int getNextHealthThreshold() {
		if (mEvents == null) {
			return 0;
		}
		return mEvents.keySet().stream().filter(i -> i < mEventCursor).mapToInt(i -> i).max().orElse(0);
	}
}
