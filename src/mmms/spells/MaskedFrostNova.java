package mmms.spells;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;

import net.md_5.bungee.api.ChatColor;

public class MaskedFrostNova
{
	private Plugin plugin;

	public MaskedFrostNova(mmbf.main.Main plugin2)
	{
		plugin = plugin2;
	}

	int w = 0;
	Random rand = new Random();

	public boolean onSpell(CommandSender sender, String[] arg)
	{
		if (arg.length != 3)
		{
			System.out.println(ChatColor.RED + "wrong number of parameters given!\n" + ChatColor.GREEN + "Usage: " + ChatColor.DARK_GREEN + "/mobspell wither_aoe <Radius> <Power> <duration>");
			return (true);
		}
		boolean error = false;
		int radius = Integer.parseInt(arg[1]);
		if (radius < 0 || radius > 65535)
		{
			System.out.println(ChatColor.RED + "Radius must be between 0 and 65535");
			error = true;
		}
		int time = Integer.parseInt(arg[2]);
		if (time < 0 || time > 500)
		{
			System.out.println(ChatColor.RED + "Power must be between 0 and 500 (ticks)");
			error = true;
		}
		if (error)
			return (true);

		spell(sender, radius, time);
		return true;
	}

	public void spell(CommandSender sender, int radius, int time)
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
		Location lLoc = launcher.getLocation();
		Player plist_targets[] = new Player[20];
		for (int i = 0; i < 20; i++)
		{
			plist_targets[i] = null;
		}
		int counter1 = 0;
		for (Player player : Bukkit.getServer().getOnlinePlayers())
		{
			plist_targets[counter1] = player;
			counter1++;
		}
		animation(radius, time, lLoc, launcher);
		deal_damage(radius, time, plist_targets, launcher);
	}

	void        deal_damage(int radius, int time, Player plist[], Entity launcher)
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable dealer = new Runnable()
		{
			@Override
			public void run()
			{
				for (int i = 0; i < 20; i++)
				{
					if (plist[i] != null)
					{
						double distance = plist[i].getLocation().distance(launcher.getLocation());
						if (distance < radius)
						{
							plist[i].addPotionEffect((new PotionEffect(PotionEffectType.HARM, 1, 2)));
							plist[i].addPotionEffect((new PotionEffect(PotionEffectType.SLOW, 8 * 20, 4)));
						}
					}
				}
			}
		};
		scheduler.scheduleSyncDelayedTask(this.plugin, dealer , (long)time);
	}

	void animation(int radius, int time, Location loc, Entity launcher)
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();

		Runnable anim_loop = new Runnable()
		{
			@Override
			public void run()
			{
				Location centerLoc = new Location(loc.getWorld(), loc.getX(), loc.getY() + 1, loc.getZ());
				launcher.teleport(new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ()));
				centerLoc.getWorld().playSound(centerLoc, Sound.BLOCK_SNOW_STEP, ((float)radius / 7), (float)(0.5 + rand.nextInt(150) / 100));
				centerLoc.getWorld().spawnParticle(Particle.SNOWBALL, centerLoc, 10, 1, 1, 1, 0.01);
			}
		};

		Runnable anim_loop2 = new Runnable()
		{
			@Override
			public void run()
			{
				Location lloc = launcher.getLocation();
				double precision = rand.nextInt(50) + 100;
				double increment = (2 * Math.PI) / precision;
				Location particleLoc = new Location(lloc.getWorld(), 0, lloc.getY() + 1.5, 0);
				double rad = (double)(radius * w) / 5;
				double angle = 0;
				for (int j = 0; j < precision; j++)
				{
					angle = (double)j * increment;
					particleLoc.setX(lloc.getX() + (rad * Math.cos(angle)));
					particleLoc.setZ(lloc.getZ() + (rad * Math.sin(angle)));
					particleLoc.setY(lloc.getY() + 1.5);
					particleLoc.getWorld().spawnParticle(Particle.SNOWBALL, particleLoc, 1, 0.02, 1.5 * rad, 0.02, 0);
				}
				if (w == 0)
				{
					particleLoc.getWorld().playSound(particleLoc, Sound.ENTITY_WITHER_SHOOT, ((float)radius / 7), 0.77F);
					particleLoc.getWorld().playSound(particleLoc, Sound.ENTITY_WITHER_SHOOT, ((float)radius / 7), 0.5F);
					particleLoc.getWorld().playSound(particleLoc, Sound.ENTITY_WITHER_SHOOT, ((float)radius / 7), 0.65F);
				}
				w++;
			}
		};

		for (int i = 0; i < time; i++)
			scheduler.scheduleSyncDelayedTask(this.plugin, anim_loop , i);
		for (int i = 0; i < 6; i++)
			scheduler.scheduleSyncDelayedTask(this.plugin, anim_loop2 , i + time);
	}
}
