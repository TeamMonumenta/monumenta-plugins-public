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

public class AxtalTntThrow{
	
private Plugin plugin;
	
	public AxtalTntThrow(mmbf.main.Main plugin2)
	{	
		plugin = plugin2;
	}
	
	Random rand = new Random();
	
	public boolean onSpell(CommandSender sender, String[] arg)
	{
		if (arg.length != 3)
		{
			System.out.println(ChatColor.RED + "wrong number of parameters given!\n" + ChatColor.GREEN + "Usage: " + ChatColor.DARK_GREEN + "/mobspell Tnt_Throw <Count> <Cooldown>");
			return (true);
		}
		boolean error = false;
		int count = Integer.parseInt(arg[1]);
		if (count < 0 || count > 64)
		{
			System.out.println(ChatColor.RED + "Count must be between 0 and 64");
			error = true;
		}
		int cooldown = Integer.parseInt(arg[2]);
		if (cooldown < 0 || cooldown > 60)
		{
			System.out.println(ChatColor.RED + "Cooldown must be between 0 and 60");
			error = true;
		}
		if (error)
			return (true);
		
		spell(sender, count, cooldown);
		return true;
	}
	
	public void spell(CommandSender sender, int count, int cooldown)
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
		List<Player> players = playersInRange(launcher.getLocation(), 140);
		launch(sender, launcher, players, count, cooldown);
		animation(launcher, count, cooldown);
	}
	
	public List<Player> playersInRange(Location loc, double range)
	{
		List<Player> out = new ArrayList<Player>();
		
		for(Player player : Bukkit.getServer().getOnlinePlayers())
		{
			if (player.getLocation().distance(loc) < range && player.getGameMode() == GameMode.SURVIVAL)
			{
				out.add(player);
			}
		}
		return (out);
	}
	
	public void animation(Entity launcher, int count, int cooldown)
	{
		Location loc = launcher.getLocation();
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable particles1 = new Runnable() {
			@Override
            public void run() {
				launcher.teleport(loc);
				loc.getWorld().spawnParticle(Particle.LAVA, loc, 4, 0, 0, 0, 0.01);
			}
		};
		Runnable particles2 = new Runnable() {
			@Override
            public void run() {
				loc.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, loc, 4, 0, 0, 0, 0.07);
				loc.getWorld().playSound(loc, Sound.ENTITY_IRONGOLEM_HURT, 1, 0.77F);
			}
		};
		loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_PIG_ANGRY, 1, 0.77F);
		for (int i = 0; i < (40 + count * cooldown); i++)
    			scheduler.scheduleSyncDelayedTask(this.plugin, particles1, (long)(i));
		for (int i = 0; i < count; i++)
			scheduler.scheduleSyncDelayedTask(this.plugin, particles2, (long)(40 + i * cooldown));
	}
	
	public void launch(CommandSender sender, Entity launcher, List<Player> plist, int count, int cooldown)
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable single_launch = new Runnable() {
			@Override
            public void run() {
				Player Target = plist.get(rand.nextInt(plist.size()));
				Location SLoc = launcher.getLocation();
				SLoc.setY(SLoc.getY() + 1.7f);
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "summon tnt " + SLoc.getX() + " " + SLoc.getY() + " " + SLoc.getZ() + " {Fuse:50}");
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "summon falling_block " + SLoc.getX() + " " + SLoc.getY() + " " + SLoc.getZ() + " {Block:leaves,Data:3,Time:1}");
				List<Entity> tnt = launcher.getNearbyEntities(0.2, 2.5, 0.2);
				Location pLoc = Target.getLocation();
				Location tLoc = tnt.get(0).getLocation();
				Vector vect = new Vector(pLoc.getX() - tLoc.getX(), 0, pLoc.getZ() - tLoc.getZ());
				vect.normalize().multiply((pLoc.distance(tLoc))/20).setY(0.7f);
				tnt.get(0).setVelocity(vect);
				tnt.get(1).setVelocity(vect);
			}
		};
		for (int i = 0; i < count; i++)
    		scheduler.scheduleSyncDelayedTask(this.plugin, single_launch , (long)(40 + i * cooldown));
	}
}

