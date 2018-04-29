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

public class SpellAxtalDeathRay implements Spell
{
	private Plugin mPlugin;
	private Entity mLauncher;
	private Random mRand = new Random();
	private int g_sound = 0;

	public SpellAxtalDeathRay(Plugin plugin, Entity launcher)
	{
		mPlugin = plugin;
		mLauncher = launcher;
	}

	@Override
	public void run()
	{
		List<Player> players = Utils.playersInRange(mLauncher.getLocation(), 60);
		Player target = players.get(mRand.nextInt(players.size()));
		launch(target);
		animation(target);
	}

	// TODO: These two methods need to be combined...
	public void launch(Player target)
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable teleport = new Runnable()
		{
			@Override
			public void run()
			{
				Location launLoc = mLauncher.getLocation().add(0, 1.6f, 0);
				Location tarLoc = target.getLocation().add(0, 0.6f, 0);
				Vector vect = new Vector(tarLoc.getX() - launLoc.getX(), tarLoc.getY() - launLoc.getY(), tarLoc.getZ() - launLoc.getZ());
				vect.normalize();
				Location summonLoc = tarLoc;
				for (int i = 0; i < 200; i++)
				{
					double dist_to_player = launLoc.distance(tarLoc);
					Vector baseVect = new Vector(vect.getX() / (400 / dist_to_player), vect.getY() / (400 / dist_to_player), vect.getZ() / (400 / dist_to_player));
					Location tmpLoc = launLoc;
					tmpLoc = launLoc;
					Vector tmpVect = baseVect;
					tmpVect = baseVect;
					tmpLoc.add(tmpVect.multiply((double)i));
					tmpLoc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, tmpLoc, 1, 0.02, 0.02, 0.02, 0);
					summonLoc = tmpLoc;
					if (tmpLoc.getBlock().getType().isSolid())
						break;
					else if (launLoc.distance(tmpLoc) > launLoc.distance(tarLoc))
						break;
					else if (tarLoc.distance(tmpLoc) < 0.5)
						break;
				}
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "summon tnt " + summonLoc.getX() + " " + summonLoc.getY() + " " + summonLoc.getZ() + " {Fuse:1}");
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "summon tnt " + summonLoc.getX() + " " + summonLoc.getY() + " " + summonLoc.getZ() + " {Fuse:3}");
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "summon tnt " + summonLoc.getX() + " " + summonLoc.getY() + " " + summonLoc.getZ() + " {Fuse:5}");
			}
		};
		scheduler.scheduleSyncDelayedTask(mPlugin, teleport, 140);
	}

	public void animation(Player target)
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable teleport = new Runnable()
		{
			@Override
			public void run()
			{
				Location launLoc = mLauncher.getLocation().add(0, 1.6f, 0);
				Location tarLoc = target.getLocation().add(0, 0.6f, 0);
				tarLoc.getWorld().playSound(tarLoc, Sound.UI_TOAST_IN, 2, (0.5f + ((float)g_sound / 200f) * 1.5f));
				launLoc.getWorld().playSound(launLoc, Sound.UI_TOAST_IN, 2, (0.5f + ((float)g_sound / 200f) * 1.5f));
				tarLoc.getWorld().playSound(tarLoc, Sound.ENTITY_WITHER_SPAWN, 2, (0.5f + ((float)g_sound / 200f) * 1.5f));
				launLoc.getWorld().playSound(launLoc, Sound.UI_TOAST_IN, 2, (0.5f + ((float)g_sound / 200f) * 1.5f));
				g_sound++;
				Vector vect = new Vector(tarLoc.getX() - launLoc.getX(), tarLoc.getY() - launLoc.getY(), tarLoc.getZ() - launLoc.getZ());
				vect.normalize();
				for (int i = 0; i < 200; i++)
				{
					double dist_to_player = launLoc.distance(tarLoc);
					Vector baseVect = new Vector(vect.getX() / (100 / dist_to_player), vect.getY() / (100 / dist_to_player), vect.getZ() / (100 / dist_to_player));
					Location tmpLoc = launLoc;
					tmpLoc = launLoc;
					Vector tmpVect = baseVect;
					tmpVect = baseVect;
					tmpLoc.add(tmpVect.multiply((double)i));
					tmpLoc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, tmpLoc, 1, 0.02, 0.02, 0.02, 0);
					tmpLoc.getWorld().spawnParticle(Particle.SMOKE_LARGE, tmpLoc, 1, 0.02, 0.02, 0.02, 0);
					tmpLoc.getWorld().spawnParticle(Particle.SPELL_MOB, tmpLoc, 1, 0.02, 0.02, 0.02, 1);
					if (tmpLoc.getBlock().getType().isSolid())
						break;
					else if (launLoc.distance(tmpLoc) > launLoc.distance(tarLoc))
						break;
					else if (tarLoc.distance(tmpLoc) < 0.5)
						break;
				}
			}
		};
		for (int j = 0; j < 140; j++)
			scheduler.scheduleSyncDelayedTask(mPlugin, teleport, j);
	}
}
