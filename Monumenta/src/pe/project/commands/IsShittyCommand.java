package pe.project.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import pe.project.protection.stopbeingshitty.StopBeingShitty;

public class IsShittyCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {	
    	if (arg0 instanceof Player) {
    		if (arg3.length < 1 || arg3.length > 1) {
	    		return false;
	    	}
    		
    		Player commandPlayer = Bukkit.getPlayer(arg3[0]);
    		if (commandPlayer != null && commandPlayer.isOnline()) {
    			StopBeingShitty.chanceOfBeingShitty((Player)arg0, commandPlayer);
    		}
    	}
    	
    	 return true;
    }
}
