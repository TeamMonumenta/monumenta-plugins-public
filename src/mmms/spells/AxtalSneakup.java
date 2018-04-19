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

public class AxtalSneakup {

	private Plugin plugin;

	public AxtalSneakup(mmbf.main.Main plugin2)
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
		List<Player> players = playersInRange(launcher.getLocation(), 80);
		Player target = players.get(rand.nextInt(players.size()));
		launch(launcher, target);
		animation(launcher, target);
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

	public void launch(Entity launcher, Player target)
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable teleport = new Runnable() {
			@Override
            public void run() {
				Location newloc = target.getLocation();
				Vector vect = newloc.getDirection().multiply(-3.0f);
				newloc.add(vect).setY(target.getLocation().getY() + 0.1f);
				launcher.teleport(newloc);
			}
		};
		scheduler.scheduleSyncDelayedTask(this.plugin, teleport, 50);
	}

	public void animation(Entity Launcher, Player target)
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITCH_AMBIENT, 1.4f, 0.5f);
		Runnable teleport = new Runnable() {
			@Override
            public void run() {
				Launcher.getWorld().playSound(Launcher.getLocation(), Sound.ENTITY_ENDERMEN_TELEPORT, 3f, 0.7f);
			}
		};
		Runnable particle = new Runnable() {
			@Override
            public void run() {
				Location particleLoc = Launcher.getLocation().add(new Location(Launcher.getWorld(), -0.5f, 0f, 0.5f));
				particleLoc.getWorld().spawnParticle(Particle.PORTAL, particleLoc, 10, 1, 1, 1, 0.03);
			}
		};
		scheduler.scheduleSyncDelayedTask(this.plugin, teleport, 49);
		for (int i = 0; i < 50; i++)
			scheduler.scheduleSyncDelayedTask(this.plugin, particle, i);
	}
}
