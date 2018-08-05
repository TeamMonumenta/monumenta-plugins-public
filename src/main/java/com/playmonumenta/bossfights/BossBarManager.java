package com.playmonumenta.bossfights;

import java.util.Map;

import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class BossBarManager
{
	private LivingEntity mBoss;
	private int mRange;
	private Map<Integer, String> mEvents;
	private int mEventCursor;
	private BossBar mBar;

	public BossBarManager(LivingEntity boss, int range, BarColor color, BarStyle style, Map<Integer, String>events)
	{
		mBoss = boss;
		mRange = range;
		mEvents = events;
		mEventCursor = 100;

		mBar = Bukkit.getServer().createBossBar(boss.getCustomName(), color, style, BarFlag.CREATE_FOG, BarFlag.DARKEN_SKY, BarFlag.PLAY_BOSS_MUSIC);
		mBar.setVisible(true);

		for (Player player : Bukkit.getServer().getOnlinePlayers())
			if (player.getLocation().distance(boss.getLocation()) < range)
				mBar.addPlayer(player);
	}

	public void update()
	{
		if (mBoss.getHealth() <= 0)
			mBar.setVisible(false);

		for (Player player : Bukkit.getServer().getOnlinePlayers())
		{
			if (player.getLocation().distance(mBoss.getLocation()) < mRange)
				mBar.addPlayer(player);
			else
				mBar.removePlayer(player);
		}

		double progress = mBoss.getHealth() / mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();

		while (mEvents != null && mEventCursor > (progress * 100))
		{
			String event = mEvents.get(mEventCursor);
			if (event != null && !event.isEmpty())
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), event);
			mEventCursor--;
		}

		mBar.setProgress(progress);
	}

	public void remove()
	{
		mBar.setVisible(false);
	}
}
