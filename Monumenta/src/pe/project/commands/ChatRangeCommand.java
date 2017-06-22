package pe.project.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import pe.project.utils.ScoreboardUtils;

public class ChatRangeCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {	
    	if (arg0 instanceof Player) {
	    	if (arg3.length < 1) {
	    		arg0.sendMessage(ChatColor.RED + "Please specify a range!");
	    		arg0.sendMessage(ChatColor.RED + "Usage: " + arg1.getUsage());
	    		return true;
	    	} else if (arg3.length > 1) {
	    		arg0.sendMessage(ChatColor.RED + "Too many parameters!");
	    		arg0.sendMessage(ChatColor.RED + "Usage: " + arg1.getUsage());
	    		return true;
	    	}
	    	
	    	int distance;
	    	
	    	try{
	    		distance = Integer.parseInt(arg3[0]);
	    	} catch (NumberFormatException e) {
	    		arg0.sendMessage(ChatColor.RED + "Invalid parameter. Must be whole number value between 0 and " + Integer.MAX_VALUE);
	    		arg0.sendMessage(ChatColor.RED + "Usage: " + arg1.getUsage());
	    		return true;
	    	}
	    	
	    	if (distance < 0 || distance > Integer.MAX_VALUE) {
	    		arg0.sendMessage(ChatColor.RED + "Invalid parameter. Must be whole number value between 0 and " + Integer.MAX_VALUE);
	    		arg0.sendMessage(ChatColor.RED + "Usage: " + arg1.getUsage());
	    		return true;
	    	}
	    	
	        ScoreboardUtils.setScoreboardValue((Player)arg0, "chatDistance", distance);
	        arg0.sendMessage(ChatColor.YELLOW + "Your chat range has been set to " + distance);
    	}
    	
    	 return true;
    }
}
