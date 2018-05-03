package pe.bossfights.bosses;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import pe.bossfights.BossBarManager;
import pe.bossfights.SpellManager;
import pe.bossfights.spells.Spell;
import pe.bossfights.utils.Utils;

public abstract class Boss
{
	LivingEntity mBoss;
	BossBarManager mBossBar;
	int mTaskIDpassive = -1;
	int mTaskIDactive = -1;

	public void constructBoss(Plugin plugin, String identityTag, LivingEntity boss, SpellManager activeSpells,
	                          List<Spell> passiveSpells, int detectionRange, BossBarManager bossBar)
	{
		mBoss = boss;
		mBossBar = bossBar;

		mBoss.setRemoveWhenFarAway(false);
		mBoss.addScoreboardTag(identityTag);

		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable passive = new Runnable()
		{
			@Override
			public void run()
			{
				/* Don't progress if players aren't present */
				if (Utils.playersInRange(mBoss.getLocation(), detectionRange).isEmpty())
					return;

				if (mBossBar != null)
					mBossBar.update();

				if (passiveSpells != null)
					for (Spell spell : passiveSpells)
						spell.run();
			}
		};
		mTaskIDpassive = scheduler.scheduleSyncRepeatingTask(plugin, passive, 1L, 5L);

		if (activeSpells != null)
		{
			Runnable active = new Runnable()
			{
				@Override
				public void run()
				{
					/* Don't progress if players aren't present */
					if (Utils.playersInRange(mBoss.getLocation(), detectionRange).isEmpty())
						return;

					activeSpells.runNextSpell();
				}
			};
			mTaskIDactive = scheduler.scheduleSyncRepeatingTask(plugin, active, 100L, 160L);
		}
	}

	/*
	 * Called only the first time the boss is summoned into the world
	 *
	 * Useful to set the bosses health / armor / etc. based on # of players
	 */
	public void init() {};

	/*
	 * Called when the boss dies
	 *
	 * Useful to use setblock or a command to trigger post-fight logic
	 */
	public void death() {};

	/*
	 * Called when the mob is unloading and we need to save its metadata
	 *
	 * Needed whenever the boss needs more parameters to instantiate than just
	 * the boss mob itself (tele to spawn location, end location to set block, etc.)
	 */
	public String serialize()
	{
		return null;
	}

	/*
	 * Called when the chunk the boss is in unloads. Also called after death()
	 *
	 * Probably don't need to override this method, but if you do, call it
	 * via super.unload()
	 */
	public void unload()
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		if (mTaskIDpassive != -1)
			scheduler.cancelTask(mTaskIDpassive);
		if (mTaskIDactive != -1)
			scheduler.cancelTask(mTaskIDactive);
		if (mBossBar != null)
			mBossBar.remove();

		if (mBoss.isValid() && mBoss.getHealth() > 0)
		{
			String content = serialize();
			if (content != null && !content.isEmpty())
			{
				//TODO: Call utility which writes data to mob
			}
		}
	}
}
