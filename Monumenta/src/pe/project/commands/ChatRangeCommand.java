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
				printUsage(arg0, arg1);
				return true;
			} else if (arg3.length > 1) {
				arg0.sendMessage(ChatColor.RED + "Too many parameters!");
				printUsage(arg0, arg1);
				return true;
			}

			int distance;

			try{
				distance = Integer.parseInt(arg3[0]);
			} catch (NumberFormatException e) {
				arg0.sendMessage(ChatColor.RED + "Invalid parameter. Must be whole number value between -1 and " + Integer.MAX_VALUE);
				printUsage(arg0, arg1);
				return true;
			}

			if ((distance < (-1)) || (distance > Integer.MAX_VALUE)) {
				arg0.sendMessage(ChatColor.RED + "Invalid parameter. Must be whole number value between 0 and " + Integer.MAX_VALUE);
				printUsage(arg0, arg1);
				return true;
			}

			ScoreboardUtils.setScoreboardValue((Player)arg0, "chatDistance", distance);
			arg0.sendMessage(ChatColor.YELLOW + "Your chat range has been set to " + distance);
		}

		 return true;
    }

	private void printUsage(CommandSender arg0, Command arg1) {
		arg0.sendMessage(ChatColor.RED + "Usage: " + arg1.getUsage());
		arg0.sendMessage(ChatColor.RED + "A range of -1 means you will never see any chat messages");
		arg0.sendMessage(ChatColor.RED + "A range of 0 disables this feature and means you will see all chat messages");
	}
}
