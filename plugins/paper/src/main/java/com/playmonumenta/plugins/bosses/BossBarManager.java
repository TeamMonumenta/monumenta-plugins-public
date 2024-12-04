package com.playmonumenta.plugins.bosses;

import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
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

	public BossBarManager(LivingEntity boss, int range, BossBar.Color color, BossBar.Overlay style, @Nullable Map<Integer, BossHealthAction> events) {
		this(boss, range, color, style, events, true);
	}

	public BossBarManager(LivingEntity boss, int range, BossBar.Color color, BossBar.Overlay style, @Nullable Map<Integer, BossHealthAction> events, boolean bossFog) {
		this(boss, range, color, style, events, bossFog, true);
	}

	public BossBarManager(LivingEntity boss, int range, BossBar.Color color, BossBar.Overlay style, @Nullable Map<Integer, BossHealthAction> events, boolean bossFog, boolean capDamage) {
		mBoss = boss;
		mRange = range;
		mEvents = events;
		mEventCursor = 100;
		mCapDamage = capDamage;
		double progress = mBoss.getHealth() / EntityUtils.getMaxHealth(mBoss);
		while (mEvents != null && mEventCursor > (progress * 100)) {
			mEventCursor--;
		}

		mBar = BossBar.bossBar(Component.text(mBoss.getName()), (float) 0, color, style, Set.of(BossBar.Flag.PLAY_BOSS_MUSIC));
		if (bossFog) {
			mBar.addFlag(BossBar.Flag.CREATE_WORLD_FOG);
			mBar.addFlag(BossBar.Flag.DARKEN_SCREEN);
		}
		boss.getWorld().showBossBar(mBar);

		for (Player player : mBoss.getWorld().getPlayers()) {
			if (player.getLocation().distance(mBoss.getLocation()) < mRange) {
				mBar.addViewer(player);
			}
		}
	}

	public void update() {
		if (mBoss.getHealth() <= 0) {
			mBoss.getWorld().hideBossBar(mBar);
		}

		for (Player player : mBoss.getWorld().getPlayers()) {
			if (player.getLocation().distance(mBoss.getLocation()) < mRange) {
				mBar.addViewer(player);
			} else {
				mBar.removeViewer(player);
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
						float cap = mEventCursor / 100.f;
						mBoss.setHealth(maxHealth * cap);
						mBar.progress(cap);
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
						float cap = mEventCursor / 100.f;
						mBoss.setHealth(maxHealth * cap);
						mBar.progress(cap);
					}
				}
				mEventCursor--;
			}
		}

		if (!Double.isFinite(progress) || progress > 1.0f || progress < 0f) {
			MMLog.warning("Boss '" + mBoss.getName() + "' has invalid health " +
				mBoss.getHealth() + " out of max " + maxHealth);
		} else {
			mBar.progress((float) progress);
		}
	}

	public void setTitle(String newTitle) {
		mBar.name(Component.text(newTitle));
	}

	public void setColor(BossBar.Color barColor) {
		mBar.color(barColor);
	}

	public void remove() {
		mBoss.getWorld().hideBossBar(mBar);
	}

	public boolean capsDamage() {
		return mCapDamage;
	}

	// Returns the highest health percentage (0 through 100) lower than current bar progress with an unused
    // BossHealthAction
	// If none exist, returns 0
	public int getNextHealthThreshold() {
		if (mEvents == null) {
			return 0;
		}
		return mEvents.keySet().stream().filter(i -> i < mEventCursor).mapToInt(i -> i).max().orElse(0);
	}
}
