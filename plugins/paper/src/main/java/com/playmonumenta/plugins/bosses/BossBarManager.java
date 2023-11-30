package com.playmonumenta.plugins.bosses;

import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class BossBarManager {
	@FunctionalInterface
	public interface BossHealthAction {
		void run(LivingEntity boss);
	}

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mRange;
	private final @Nullable Map<Integer, BossHealthAction> mEvents;
	private final BossBar mBar;
	private int mEventCursor;
	private final boolean mCapDamage;

	public BossBarManager(Plugin plugin, LivingEntity boss, int range, BarColor color, BarStyle style, @Nullable Map<Integer, BossHealthAction> events) {
		this(plugin, boss, range, color, style, events, true);
	}

	public BossBarManager(Plugin plugin, LivingEntity boss, int range, BarColor color, BarStyle style, @Nullable Map<Integer, BossHealthAction> events, boolean bossFog) {
		this(plugin, boss, range, color, style, events, bossFog, true);
	}

	public BossBarManager(Plugin plugin, LivingEntity boss, int range, BarColor color, BarStyle style, @Nullable Map<Integer, BossHealthAction> events, boolean bossFog, boolean capDamage) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mEvents = events;
		mEventCursor = 100;
		mCapDamage = capDamage;
		double progress = mBoss.getHealth() / EntityUtils.getMaxHealth(mBoss);
		while (mEvents != null && mEventCursor > (progress * 100)) {
			mEventCursor--;
		}

		if (bossFog) {
			mBar = Bukkit.getServer().createBossBar(boss.getName(), color, style, BarFlag.CREATE_FOG, BarFlag.DARKEN_SKY, BarFlag.PLAY_BOSS_MUSIC);
		} else {
			mBar = Bukkit.getServer().createBossBar(boss.getName(), color, style, BarFlag.PLAY_BOSS_MUSIC);
		}
		mBar.setVisible(true);

		for (Player player : boss.getWorld().getPlayers()) {
			if (player.getLocation().distance(boss.getLocation()) < range) {
				mBar.addPlayer(player);
			}
		}
	}

	public void update() {
		if (mBoss.getHealth() <= 0) {
			mBar.setVisible(false);
		}

		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			if (player.getWorld().equals(mBoss.getWorld()) && player.getLocation().distance(mBoss.getLocation()) < mRange) {
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
			mPlugin.getLogger().severe("Boss '" + mBoss.getName() + "' has invalid health " +
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
}
