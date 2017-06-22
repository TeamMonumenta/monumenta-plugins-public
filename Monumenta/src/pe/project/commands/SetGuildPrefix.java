package pe.project.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class SetGuildPrefix implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		if (arg3.length < 1) {
    		arg0.sendMessage(ChatColor.RED + "No prefix arguement!");
    		arg0.sendMessage(ChatColor.RED + "Usage: " + arg1.getUsage());
    		return false;
    	} else if (arg3.length > 4) {
    		arg0.sendMessage(ChatColor.RED + "Too many parameters!");
    		arg0.sendMessage(ChatColor.RED + "Usage: " + arg1.getUsage());
    		return false;
    	} else if (arg3[1].length() >= 15) {
    		arg0.sendMessage(ChatColor.RED + "Guild Tag is too long!");
    	}
		
		Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		if (scoreboard != null) {
			Team team = scoreboard.getTeam(arg3[0]);
			if (team != null) {
				if (arg3[2] != null) {
					ChatColor color = ChatColor.valueOf(arg3[2]);
					if (color != null) {
						String second = "";
						if (arg3.length > 3 && arg3[3] != null) {
							ChatColor secondColor = ChatColor.valueOf(arg3[3]);
							if (secondColor != null) {
								second = "" + secondColor;
							}
						}
						
						team.setPrefix(color + arg3[1] + ChatColor.RESET + second + " ");
					}
				}
			}
		} else {
			arg0.sendMessage(ChatColor.RED + "No team with this name!");
		}
		
		return true;
	}

}
