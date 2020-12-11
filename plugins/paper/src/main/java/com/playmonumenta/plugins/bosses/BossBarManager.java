package com.playmonumenta.plugins.bosses;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BossBarManager {
	@FunctionalInterface
	public interface BossHealthAction {
		void run(LivingEntity boss);
	}

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mRange;
	private final Map<Integer, BossHealthAction> mEvents;
	private final BossBar mBar;
	private int mEventCursor;

	public BossBarManager(Plugin plugin, LivingEntity boss, int range, BarColor color, BarStyle style, Map<Integer, BossHealthAction> events) {
		this(plugin, boss, range, color, style, events, true);
	}

	public BossBarManager(Plugin plugin, LivingEntity boss, int range, BarColor color, BarStyle style, Map<Integer, BossHealthAction> events, boolean bossFog) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mEvents = events;
		mEventCursor = 100;
		double progress = mBoss.getHealth() / mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
		while (mEvents != null && mEventCursor > (progress * 100)) {
			mEventCursor--;
		}

		if (bossFog) {
			mBar = Bukkit.getServer().createBossBar(boss.getCustomName(), color, style, BarFlag.CREATE_FOG, BarFlag.DARKEN_SKY, BarFlag.PLAY_BOSS_MUSIC);
		} else {
			mBar = Bukkit.getServer().createBossBar(boss.getCustomName(), color, style, BarFlag.PLAY_BOSS_MUSIC);
		}
		mBar.setVisible(true);

		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
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
			if (player.getLocation().distance(mBoss.getLocation()) < mRange) {
				mBar.addPlayer(player);
			} else {
				mBar.removePlayer(player);
			}
		}

		double progress = mBoss.getHealth() / mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
		if (mEvents != null && (progress * 100) > 99 && mEventCursor >= 99) {
			BossHealthAction event = mEvents.get(mEventCursor);
			if (event != null) {
				event.run(mBoss);
			}
			mEventCursor--;
		}
		while (mEvents != null && mEventCursor > (progress * 100)) {
			BossHealthAction event = mEvents.get(mEventCursor);
			if (event != null) {
				event.run(mBoss);
			}
			mEventCursor--;
		}

		if (progress > 1.0f || progress < 0f) {
			mPlugin.getLogger().severe("Boss '" + mBoss.getCustomName() + "' has invalid health " +
			                           Double.toString(mBoss.getHealth()) + " out of max " +
									   Double.toString(mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue()));
		} else {
			mBar.setProgress(progress);
		}
	}

	public void remove() {
		mBar.setVisible(false);
	}
}
