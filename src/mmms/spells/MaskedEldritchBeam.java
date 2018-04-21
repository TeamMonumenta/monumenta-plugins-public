package mmms.spells;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

import mmbf.utils.Utils;
import net.md_5.bungee.api.ChatColor;

public class MaskedEldritchBeam
{

	private Plugin plugin;

	public MaskedEldritchBeam(mmbf.main.Main plugin2)
	{
		plugin = plugin2;
	}

	Random rand = new Random();
	Utils utils = new Utils(plugin);

	int anim_task_id[] = new int[20];
	int dmg_task_id[] = new int[20];

	public boolean onSpell(CommandSender sender, String[] arg)
	{
		if (arg.length != 1)
		{
			System.out.println(ChatColor.RED + "wrong number of parameters given!\n" + ChatColor.GREEN + "Usage: " + ChatColor.DARK_GREEN + "/mobspell Tnt_Throw <Count> <Cooldown>");
			return (true);
		}
		boolean error = false;
		if (error)
			return (true);

		spell(sender);
		return true;
	}

	public void spell(CommandSender sender)
	{
		Entity launcher = null;

		if (sender instanceof Entity)
			launcher = (Entity)sender;
		else if (sender instanceof ProxiedCommandSender)
		{
			CommandSender callee = ((ProxiedCommandSender)sender).getCallee();
			if (callee instanceof Entity)
				launcher = (Entity)callee;
		}
		if (launcher == null)
		{
			System.out.println("wither_aoe spell failed");
			return ;
		}
		System.out.println("hey");
		int id = 0;
		for (Player player : Utils.playersInRange(launcher.getLocation(), 40))
		{
			launch(launcher, player, id);
			animation(launcher, player, id);
			id++;
		}
	}

	public void launch(Entity launcher, Player target, int id)
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
		dmg_task_id[id] = scheduler.scheduleSyncRepeatingTask(plugin, damage, 0L, 20L);
	}

	int g_sound = 0;

	public void animation(Entity launcher, Player target, int id)
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable teleport = new Runnable()
		{
			@Override
			public void run()
			{
				Location launLoc = launcher.getLocation().add(0, 1.6f, 0);
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
					else
					{
						if (tarLoc.distance(tmpLoc) < 0.5)
							break;
					}
				}
				if (g_sound >= 80)
				{
					scheduler.cancelTask(anim_task_id[id]);
					scheduler.cancelTask(dmg_task_id[id]);
				}
			}
		};
		anim_task_id[id] = scheduler.scheduleSyncRepeatingTask(plugin, teleport, 0L, 2L);
	}

}
