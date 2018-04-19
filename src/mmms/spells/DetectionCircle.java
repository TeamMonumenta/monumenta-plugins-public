package mmms.spells;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import mmbf.utils.Utils;
import net.md_5.bungee.api.ChatColor;

public class DetectionCircle
{

	private Plugin plugin;

	public DetectionCircle(mmbf.main.Main plugin2)
	{
		plugin = plugin2;
	}

	Random rand = new Random();
	int runs_left;
	int taskID;

	public boolean onSpell(CommandSender sender, String[] arg)
	{
		if (arg.length != 9)
		{
			System.out.println(ChatColor.RED + "wrong number of parameters given!\n" + ChatColor.GREEN + "Usage: " + ChatColor.DARK_GREEN + "/mobspell wither_aoe <Radius> <Power> <duration>");
			return (true);
		}
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
			return (true);
		}
		int radius = Integer.parseInt(arg[4]);
		if (radius < 0 || radius > 65535)
			System.out.println(ChatColor.RED + "Radius must be between 0 and 65535");
		int duration = Integer.parseInt(arg[5]);
		if (duration < 0 || duration > 65535)
			System.out.println(ChatColor.RED + "Duration must be between 0 and 65535");

		Utils utils = new Utils(plugin);
		Location center = utils.getLocation(launcher.getLocation(), arg[1], arg[2], arg[3]);
		Location target = utils.getLocation(launcher.getLocation(), arg[6], arg[7], arg[8]);
		spell(center, target, radius, duration);
		return true;
	}

	public void spell(Location center, Location target, int radius, int duration)
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		runs_left = duration;
		Runnable loop = new Runnable()
		{
			@Override
			public void run()
			{
				int  n = rand.nextInt(50) + 100;
				double precision = n;
				double increment = (2 * Math.PI) / precision;
				Location particleLoc = new Location(center.getWorld(), 0, center.getY() + 5, 0);
				double rad = radius;
				double angle = 0;
				for (int j = 0; j < precision; j++)
				{
					angle = (double)j * increment;
					particleLoc.setX(center.getX() + (rad * Math.cos(angle)));
					particleLoc.setZ(center.getZ() + (rad * Math.sin(angle)));
					particleLoc.setY(center.getY() + 5 * (double)(rand.nextInt(120) - 60) / (60));
					particleLoc.getWorld().spawnParticle(Particle.SMOKE_LARGE, particleLoc, 1, 0.02, 0.02, 0.02, 0);
				}

				for (Player player : Bukkit.getServer().getOnlinePlayers())
				{
					if (player.getLocation().distance(center) < radius && player.getGameMode() == GameMode.SURVIVAL)
					{
						target.getBlock().setType(Material.REDSTONE_BLOCK);
						scheduler.cancelTask(taskID);
						break;
					}
				}
				if (runs_left <= 0)
					scheduler.cancelTask(taskID);
				runs_left -= 5;
			}
		};
		taskID = scheduler.scheduleSyncRepeatingTask(plugin, loop, 1L, 5L);
	}
}
