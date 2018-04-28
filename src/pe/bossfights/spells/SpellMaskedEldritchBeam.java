package pe.bossfights.spells;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.Sound;
import org.bukkit.util.Vector;

import mmbf.utils.Utils;

public class SpellMaskedEldritchBeam implements Spell
{
	private Plugin mPlugin;
	private Entity mLauncher;
	// TODO: This seriously needs cleanup...
	private int anim_task_id[] = new int[20];
	private int dmg_task_id[] = new int[20];
	private int g_sound = 0;

	public SpellMaskedEldritchBeam(Plugin plugin, Entity launcher)
	{
		mPlugin = plugin;
		mLauncher = launcher;
	}

	@Override
	public void run()
	{
		int id = 0;
		for (Player player : Utils.playersInRange(mLauncher.getLocation(), 40))
		{
			launch(player, id);
			animation(player, id);
			id++;
		}
	}

	private void launch(Player target, int id)
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable damage = new Runnable()
		{
			@Override
			public void run()
			{
				target.damage(3f);
			}
		};
		dmg_task_id[id] = scheduler.scheduleSyncRepeatingTask(mPlugin, damage, 0L, 20L);
	}

	private void animation(Player target, int id)
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable teleport = new Runnable()
		{
			@Override
			public void run()
			{
				Location launLoc = mLauncher.getLocation().add(0, 1.6f, 0);
				Location tarLoc = target.getLocation().add(0, 0.6f, 0);
				tarLoc.getWorld().playSound(tarLoc, Sound.UI_TOAST_IN, 2, (0.5f + ((float)g_sound / 80f) * 1.5f));
				launLoc.getWorld().playSound(launLoc, Sound.UI_TOAST_IN, 2, (0.5f + ((float)g_sound / 80f) * 1.5f));
				launLoc.getWorld().playSound(launLoc, Sound.UI_TOAST_IN, 2, (0.5f + ((float)g_sound / 80f) * 1.5f));
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
					tmpLoc.getWorld().spawnParticle(Particle.CLOUD, tmpLoc, 1, 0.02, 0.02, 0.02, 0);
					tmpLoc.getWorld().spawnParticle(Particle.SPELL_MOB, tmpLoc, 1, 0.02, 0.02, 0.02, 1);
					if (tmpLoc.getBlock().getType() != Material.AIR)
					{
						scheduler.cancelTask(anim_task_id[id]);
						scheduler.cancelTask(dmg_task_id[id]);
						break;
					}
					else if (launLoc.distance(tmpLoc) > launLoc.distance(tarLoc))
						break;
					else if (tarLoc.distance(tmpLoc) < 0.5)
						break;
				}
				if (g_sound >= 80)
				{
					scheduler.cancelTask(anim_task_id[id]);
					scheduler.cancelTask(dmg_task_id[id]);
				}
			}
		};
		anim_task_id[id] = scheduler.scheduleSyncRepeatingTask(mPlugin, teleport, 0L, 2L);
	}
}
