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
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.md_5.bungee.api.ChatColor;

public class AxtalWitherAoe
{
	private Plugin plugin;

	public AxtalWitherAoe(mmbf.main.Main plugin2)
	{
		plugin = plugin2;
	}

	int w = -80;
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
		int power = Integer.parseInt(arg[2]);
		if (power < 0 || power > 5)
		{
			System.out.println(ChatColor.RED + "Power must be between 0 and 5");
			error = true;
		}
		if (error)
			return (true);

		spell(sender, radius, power);
		return true;
	}

	public void spell(CommandSender sender, int radius, int power)
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
		for(Player player : Bukkit.getServer().getOnlinePlayers())
		{
			plist_targets[counter1] = player;
			counter1++;
		}
		animation(radius, lLoc, launcher);
		deal_damage(radius, power, plist_targets, launcher);
    }

	void		deal_damage(int radius, int power, Player plist[], Entity launcher)
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
								int pot_pow = (int)((double)power * (((double)radius - distance) / (double)radius));
								plist[i].addPotionEffect((new PotionEffect(PotionEffectType.HARM, 1, pot_pow)));
								plist[i].addPotionEffect((new PotionEffect(PotionEffectType.BLINDNESS, 30, 1)));
							}
						}
					}
            }
		};
		scheduler.scheduleSyncDelayedTask(this.plugin, dealer , 80L);
	}

	void animation(int radius, Location loc, Entity launcher)
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable anim_loop = new Runnable() {
            @Override
            public void run() {
					Location lloc = launcher.getLocation();
					int  n = rand.nextInt(50) + 100;
					double precision = n;
					double increment = (2 * Math.PI) / precision;
					Location particleLoc = new Location(lloc.getWorld(), 0, lloc.getY() + 1.5, 0);
					double rad = radius * (w < 0 ? ((double)w / 80) : ((double)w / 5));
					double angle = 0;
					for(int j = 0; j < precision; j++)
					{
						angle = (double)j * increment;
						particleLoc.setX(lloc.getX() + (rad * Math.cos(angle)));
						particleLoc.setZ(lloc.getZ() + (rad * Math.sin(angle)));
						particleLoc.setY(lloc.getY() + 1.5);
						particleLoc.getWorld().spawnParticle(Particle.SMOKE_LARGE, particleLoc, 1, 0.02, 1.5 * rad, 0.02, 0);
					}
					if (w < -20 && w % 2 == 0)
						particleLoc.getWorld().playSound(particleLoc, Sound.ENTITY_CAT_HISS, ((float)radius / 7), (float)(0.5 + ((float)(w + 60) / 100)));
					else if (w == -1)
					{
						particleLoc.getWorld().playSound(particleLoc, Sound.ENTITY_WITHER_SHOOT, ((float)radius / 7), 0.77F);
						particleLoc.getWorld().playSound(particleLoc, Sound.ENTITY_WITHER_SHOOT, ((float)radius / 7), 0.5F);
						particleLoc.getWorld().playSound(particleLoc, Sound.ENTITY_WITHER_SHOOT, ((float)radius / 7), 0.65F);
					}
					w++;
            }
        };

        for (int i = -80; i < 5; i++)
				scheduler.scheduleSyncDelayedTask(this.plugin, anim_loop , 1L * (i + 81));
	}
}
