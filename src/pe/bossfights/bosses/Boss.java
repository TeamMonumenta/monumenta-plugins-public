package pe.bossfights.bosses;

import java.util.List;

import mmbf.utils.SpellBossBar;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import pe.bossfights.SpellManager;
import pe.bossfights.spells.Spell;
import pe.bossfights.utils.Utils;

public abstract class Boss
{
	LivingEntity mBoss;
	SpellBossBar mBossBar;
	int mTaskIDpassive;
	int mTaskIDactive;

	public void constructBoss(Plugin plugin, String identityTag, LivingEntity boss, SpellManager activeSpells,
	                          List<Spell> passiveSpells, int detectionRange, SpellBossBar bossBar)
	{
		mBoss = boss;
		mBossBar = bossBar;

		mBoss.setRemoveWhenFarAway(false);
		mBoss.addScoreboardTag(identityTag);

		Runnable passive = new Runnable()
		{
			@Override
			public void run()
			{
				/* Don't progress if players aren't present */
				if (Utils.playersInRange(mBoss.getLocation(), detectionRange).isEmpty())
					return;

				if (mBossBar != null)
					mBossBar.update_bar(mBoss, detectionRange);

				for (Spell spell : passiveSpells)
					spell.run();
			}
		};
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

		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		mTaskIDpassive = scheduler.scheduleSyncRepeatingTask(plugin, passive, 1L, 5L);
		mTaskIDactive = scheduler.scheduleSyncRepeatingTask(plugin, active, 100L, 160L);
	}

	/* Called only the first time the boss is summoned into the world */
	public abstract void init();

	/* Called when the boss dies */
	public abstract void death();

	/* Called when the chunk the boss is in unloads. Also called after death() */
	public void unload()
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.cancelTask(mTaskIDpassive);
		scheduler.cancelTask(mTaskIDactive);
		if (mBossBar != null)
			mBossBar.remove();
	}
}
