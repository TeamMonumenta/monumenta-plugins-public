package mmms.spells;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatColor;

public class AxtalDeathRay
{

	private Plugin plugin;

	public AxtalDeathRay(mmbf.main.Main plugin2)
	{
		plugin = plugin2;
	}

	Random rand = new Random();

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
		List<Player> players = playersInRange(launcher.getLocation(), 60);
		Player target = players.get(rand.nextInt(players.size()));
		launch(launcher, target);
		animation(launcher, target);
	}

	public List<Player> playersInRange(Location loc, double range)
	{
		List<Player> out = new ArrayList<Player>();

		for (Player player : Bukkit.getServer().getOnlinePlayers())
		{
			if (player.getLocation().distance(loc) < range && player.getGameMode() == GameMode.SURVIVAL)
				out.add(player);
		}
		return (out);
	}

	public void launch(Entity launcher, Player target)
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable teleport = new Runnable()
		{
			@Override
			public void run()
			{
				Location launLoc = launcher.getLocation().add(0, 1.6f, 0);
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
					{
						System.out.println("found block after" + i + "tries");
						break;
					}
					else if (launLoc.distance(tmpLoc) > launLoc.distance(tarLoc))
						break;
					else
					{
						if (tarLoc.distance(tmpLoc) < 0.5)
						{
							System.out.println("found player after" + i + "tries");
							break;
						}
					}
					if (i == 199)
						break;
				}
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "summon tnt " + summonLoc.getX() + " " + summonLoc.getY() + " " + summonLoc.getZ() + " {Fuse:1}");
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "summon tnt " + summonLoc.getX() + " " + summonLoc.getY() + " " + summonLoc.getZ() + " {Fuse:3}");
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "summon tnt " + summonLoc.getX() + " " + summonLoc.getY() + " " + summonLoc.getZ() + " {Fuse:5}");
			}
		};
		scheduler.scheduleSyncDelayedTask(this.plugin, teleport, 140);
	}

	int g_sound = 0;

	public void animation(Entity launcher, Player target)
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable teleport = new Runnable()
		{
			@Override
			public void run()
			{
				Location launLoc = launcher.getLocation().add(0, 1.6f, 0);
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
					else
					{
						if (tarLoc.distance(tmpLoc) < 0.5)
							break;
					}
				}
			}
		};
		for (int j = 0; j < 140; j++)
			scheduler.scheduleSyncDelayedTask(this.plugin, teleport, (long)j);
	}
}
