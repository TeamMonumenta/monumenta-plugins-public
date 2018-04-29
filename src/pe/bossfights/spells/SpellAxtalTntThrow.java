package pe.bossfights.spells;

import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.Sound;
import org.bukkit.util.Vector;

import mmbf.utils.Utils;

public class SpellAxtalTntThrow implements Spell
{
	private Plugin mPlugin;
	private Entity mLauncher;
	private int mCount;
	private int mCooldown;
	Random mRand = new Random();

	public SpellAxtalTntThrow(Plugin plugin, Entity launcher, int count, int cooldown)
	{
		mPlugin = plugin;
		mLauncher = launcher;
		mCount = count;
		mCooldown = cooldown;
	}

	@Override
	public void run()
	{
		launch();
		animation();
	}

	private void animation()
	{
		Location loc = mLauncher.getLocation();
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable particles1 = new Runnable()
		{
			@Override
			public void run()
			{
				mLauncher.teleport(loc);
				loc.getWorld().spawnParticle(Particle.LAVA, loc, 4, 0, 0, 0, 0.01);
			}
		};
		Runnable particles2 = new Runnable()
		{
			@Override
			public void run()
			{
				loc.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, loc, 4, 0, 0, 0, 0.07);
				loc.getWorld().playSound(loc, Sound.ENTITY_IRONGOLEM_HURT, 1, 0.77F);
			}
		};
		loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_PIG_ANGRY, 1, 0.77F);
		for (int i = 0; i < (40 + mCount * mCooldown); i++)
			scheduler.scheduleSyncDelayedTask(mPlugin, particles1, (long)(i));
		for (int i = 0; i < mCount; i++)
			scheduler.scheduleSyncDelayedTask(mPlugin, particles2, (long)(40 + i * mCooldown));
	}

	private void launch()
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable single_launch = new Runnable()
		{
			@Override
			public void run()
			{
				List<Player> plist = Utils.playersInRange(mLauncher.getLocation(), 100);
				Player Target = plist.get(mRand.nextInt(plist.size()));
				Location SLoc = mLauncher.getLocation();
				SLoc.setY(SLoc.getY() + 1.7f);
				/* TODO: Should summon these entities directly */
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "summon tnt " + SLoc.getX() + " " + SLoc.getY() + " " + SLoc.getZ() + " {Fuse:50}");
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "summon falling_block " + SLoc.getX() + " " + SLoc.getY() + " " + SLoc.getZ() + " {Block:leaves,Data:3,Time:1}");
				List<Entity> tnt = mLauncher.getNearbyEntities(0.2, 2.5, 0.2);
				Location pLoc = Target.getLocation();
				Location tLoc = tnt.get(0).getLocation();
				Vector vect = new Vector(pLoc.getX() - tLoc.getX(), 0, pLoc.getZ() - tLoc.getZ());
				vect.normalize().multiply((pLoc.distance(tLoc)) / 20).setY(0.7f);
				tnt.get(0).setVelocity(vect);
				tnt.get(1).setVelocity(vect);
			}
		};
		for (int i = 0; i < mCount; i++)
			scheduler.scheduleSyncDelayedTask(mPlugin, single_launch, (long)(40 + i * mCooldown));
	}
}

