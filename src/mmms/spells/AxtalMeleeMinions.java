package mmms.spells;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatColor;

public class AxtalMeleeMinions
{
	private Plugin plugin;

	public AxtalMeleeMinions(mmbf.main.Main plugin2)
	{
		plugin = plugin2;
	}

	Random rand = new Random();

	public boolean onSpell(CommandSender sender, String[] arg)
	{
		if (arg.length != 4)
		{
			System.out.println(ChatColor.RED + "wrong number of parameters given!\n" + ChatColor.GREEN + "Usage: " + ChatColor.DARK_GREEN + "/mobspell Melee_Minions_1 <Count> <Scope> <Repeats>");
			return (true);
		}
		boolean error = false;
		int count = Integer.parseInt(arg[1]);
		if (count < 0 || count > 64)
		{
			System.out.println(ChatColor.RED + "Count must be between 0 and 64");
			error = true;
		}
		int scope = Integer.parseInt(arg[2]);
		if (scope < 0 || scope > 32)
		{
			System.out.println(ChatColor.RED + "Scope must be between 0 and 32");
			error = true;
		}
		int repeats = Integer.parseInt(arg[3]);
		if (repeats < 0 || repeats > 5)
		{
			System.out.println(ChatColor.RED + "Repeats must be between 0 and 5");
			error = true;
		}
		if (error)
			return (true);

		spell(sender, count, scope, repeats);
		return true;
	}

	public void spell(CommandSender sender, int count, int scope, int repeats)
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
		animation(lLoc, repeats, launcher);
		spawn(sender, launcher, lLoc, count, scope, repeats);
	}

	public void spawn(CommandSender sender, Entity esender, Location loc, int count, int scope, int repeats)
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable single_spawn = new Runnable()
		{
			@Override
			public void run()
			{
				int nb_to_spawn = count + (rand.nextInt(2 * scope) - scope);
				for (int j = 0; j < nb_to_spawn; j++)
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "summon skeleton " + loc.getX() + " " + loc.getY() + " " + loc.getZ() + " {CustomName:\"Soul\",CustomNameVisible:1,Tags:[\"Soul\"],ArmorItems:[{},{},{id:\"minecraft:leather_chestplate\",Count:1b,tag:{display:{color:12430010}}},{id:\"minecraft:skull\",Count:1b}],ArmorDropChances:[0.085F,0.085F,-327.67F,-327.67F],Attributes:[{Name:generic.maxHealth,Base:10},{Name:generic.attackDamage,Base:6}],Health:10.0f,DeathLootTable:\"empty\",ActiveEffects:[{Id:14,Amplifier:0,Duration:222220,ShowParticles:0b}],Team:\"Tlax\"}");
				for (Entity skelly : esender.getNearbyEntities(0.2, 0.2, 0.2))
				{
					if (skelly.getType() == EntityType.SKELETON)
					{
						double x = 0.5f * Math.cos(((double)rand.nextInt(628) / 100));
						double z = 0.5f * Math.sin(((double)rand.nextInt(628) / 100));
						skelly.setVelocity(new Vector(x, 0.5, z));
					}
				}
			}
		};
		for (int i = 0; i < repeats; i++)
		{
			scheduler.scheduleSyncDelayedTask(this.plugin, single_spawn, (long)(40 + 15 * i));
		}
	}

	public void animation(Location loc, int repeats, Entity launcher)
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable anim_loop = new Runnable()
		{
			@Override
			public void run()
			{
				Location centerLoc = new Location(loc.getWorld(), loc.getX(), loc.getY() + 1, loc.getZ());
				launcher.teleport(new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ()));
				centerLoc.getWorld().playSound(centerLoc, Sound.BLOCK_PORTAL_AMBIENT, 1f, 0.5f);
				centerLoc.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, centerLoc, 20, 1, 1, 1, 0.01);
			}
		};
		for (int i = 0; i < (40 + repeats * 15) / 3; i++)
			scheduler.scheduleSyncDelayedTask(this.plugin, anim_loop , (long)i * 3);
	}
}
