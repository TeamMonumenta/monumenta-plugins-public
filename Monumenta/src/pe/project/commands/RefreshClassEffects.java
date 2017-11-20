package pe.project.commands;

import java.util.Collection;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import pe.project.Plugin;

public class RefreshClassEffects implements CommandExecutor {
	Plugin mPlugin;
	World mWorld;
	
	public RefreshClassEffects(Plugin plugin, World world) {
		mPlugin = plugin;
		mWorld = world;
	}
	
	@Override
	public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		if (arg3.length < 1) {
			arg0.sendMessage(ChatColor.RED + "Too few parameters!");
    		arg0.sendMessage(ChatColor.RED + "Usage: " + arg1.getUsage());
    		return true;
    	} else if (arg3.length > 3) {
    		arg0.sendMessage(ChatColor.RED + "Too many parameters!");
    		arg0.sendMessage(ChatColor.RED + "Usage: " + arg1.getUsage());
    		return true;
    	}
		
		double x, y, z;
		try {
			x = (double)Integer.parseInt(arg3[0]) + 0.5;
			y = (double)Integer.parseInt(arg3[1]);
			z = (double)Integer.parseInt(arg3[2]) + 0.5;
		} catch (NumberFormatException e) {
			arg0.sendMessage(ChatColor.RED + "One of your position values was incorrectly formatted!");
			arg0.sendMessage(ChatColor.RED + "Usage: " + arg1.getUsage());
			return true;
		}
		
		//	Now that we got a location we want to check, grab all nearby players within a couple blocks to be safe and refresh their class abilities
		//	by calling refreshClassEffects on the potion manager.
		Collection<Entity> entities = mWorld.getNearbyEntities(new Location(mWorld, x, y, z), 2, 2, 2);
		for (Entity entity : entities) {
			if (entity instanceof Player) {
				mPlugin.mPotionManager.refreshClassEffects((Player)entity);
			}
		}
		
		return true;
	}

}
