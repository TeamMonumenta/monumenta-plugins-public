package pe.bossfights.spells;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.Sound;
import org.bukkit.util.Vector;

import pe.bossfights.utils.Utils;

public class SpellMaskedSummonBlazes implements Spell
{
	private Plugin mPlugin;
	private Entity mLauncher;
	private Random mRand = new Random();

	public SpellMaskedSummonBlazes(Plugin plugin, Entity launcher)
	{
		mPlugin = plugin;
		mLauncher = launcher;
	}

	@Override
	public void run()
	{
		Location lLoc = mLauncher.getLocation();
		int count = Utils.playersInRange(mLauncher.getLocation(), 25).size();
		count = count >= 3 ? 4 : count;
		animation(lLoc, 2);
		spawn(lLoc, count, 2);
	}

	private void spawn(Location loc, int count, int repeats)
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable single_spawn = new Runnable()
		{
			@Override
			public void run()
			{
				for (int j = 0; j < count; j++)
				{
					Entity blaz = loc.getWorld().spawnEntity(loc, EntityType.BLAZE);
					double x = 0.5f * Math.cos(((double)mRand.nextInt(628) / 100));
					double z = 0.5f * Math.sin(((double)mRand.nextInt(628) / 100));
					blaz.setVelocity(new Vector(x, 0.3, z));
				}
			}
		};
		for (int i = 0; i < repeats; i++)
			scheduler.scheduleSyncDelayedTask(mPlugin, single_spawn, 45 + 5 * i);
	}

	private void animation(Location loc, int repeats)
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable anim_loop = new Runnable()
		{
			@Override
			public void run()
			{
				Location centerLoc = new Location(loc.getWorld(), loc.getX(), loc.getY() + 1, loc.getZ());
				Location particleLoc = new Location(loc.getWorld(), 0, 0, 0);
				mLauncher.teleport(loc);
				centerLoc.getWorld().playSound(centerLoc, Sound.BLOCK_PORTAL_AMBIENT, 1f, 2f);
				for (int j = 0; j < 5; j++)
				{
					while (particleLoc.distance(centerLoc) > 2)
					{
						particleLoc.setX(loc.getX() + ((double)(mRand.nextInt(4000) - 2000) / 1000));
						particleLoc.setZ(loc.getZ() + ((double)(mRand.nextInt(4000) - 2000) / 1000));
						particleLoc.setY(loc.getY() + ((double)(mRand.nextInt(4000) - 2000) / 1000));
					}
					particleLoc.getWorld().spawnParticle(Particle.LAVA, particleLoc, 4, 0, 0, 0, 0.01);
					particleLoc.setX(0);
					particleLoc.setY(0);
					particleLoc.setZ(0);
				}
			}
		};
		for (int i = 0; i < (45 + 5 * repeats) / 3; i++)
			scheduler.scheduleSyncDelayedTask(mPlugin, anim_loop, i * 3);
	}
}
