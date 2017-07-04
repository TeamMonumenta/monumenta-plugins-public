package pe.project.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import pe.project.utils.ScoreboardUtils;

public class GetScore implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		if (arg0 instanceof Player) {
			if (arg3.length < 1) {
	    		arg0.sendMessage(ChatColor.RED + "Too few parameters!");
	    		arg0.sendMessage(ChatColor.RED + "Usage: " + arg1.getUsage());
	    		return false;
	    	} else if (arg3.length > 2) {
	    		arg0.sendMessage(ChatColor.RED + "Too many parameters!");
	    		arg0.sendMessage(ChatColor.RED + "Usage: " + arg1.getUsage());
	    		return false;
	    	}
	
			int score = ScoreboardUtils.getScoreboardValue(arg3[0], arg3[1]);
			if (score != -1) {
				((Player)arg0).sendMessage(ChatColor.GREEN + arg3[0] + "'s score for " + arg3[1] + ": " + score);
				return true;
			}
		}
		
		return false;
	}

}
