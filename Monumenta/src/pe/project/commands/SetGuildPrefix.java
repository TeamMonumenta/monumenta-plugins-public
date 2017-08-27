package pe.project.commands;

import java.util.Arrays;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class SetGuildPrefix implements CommandExecutor {
	public static String[] getEnumNames(Class<? extends Enum<?>> e) {
		return Arrays.stream(e.getEnumConstants()).map(Enum::name).toArray(String[]::new);
	}

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
    	} else if (arg3[1].length() > 7) {
    		arg0.sendMessage(ChatColor.RED + "Guild Tag is too long!");
    	}

		Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		if (scoreboard != null) {
			Team team = scoreboard.getTeam(arg3[0]);
			if (team != null) {
				if (arg3[2] != null) {
					ChatColor tagColor;
					/* Get the tag color or print an error if it is not a valid color */
					try {
						tagColor = ChatColor.valueOf(arg3[2]);
					} catch (IllegalArgumentException e) {
						arg0.sendMessage(ChatColor.RED + "Invalid tag color! Available colors are:");
						for (String s: getEnumNames(ChatColor.class)) {
							arg0.sendMessage(ChatColor.RED + "- " + s);
						}
						tagColor = null;
					}

					if (tagColor != null) {
						String nameColorString = "";

						/* Get the name color or print an error if it is not a valid color */
						if (arg3.length > 3 && arg3[3] != null) {
							ChatColor nameColor;
							try {
								nameColor = ChatColor.valueOf(arg3[3]);
							} catch (IllegalArgumentException e) {
								arg0.sendMessage(ChatColor.RED + "Invalid player color! Available colors are:");
								for (String s: getEnumNames(ChatColor.class)) {
									arg0.sendMessage(ChatColor.RED + "- " + s);
								}
								nameColor = null;
							}

							if (nameColor != null) {
								nameColorString = "" + nameColor;
							}
						}

						team.setPrefix(tagColor + arg3[1] + ChatColor.RESET + nameColorString + " ");
						team.setSuffix("" + ChatColor.RESET);
					}
				}
			}
		} else {
			arg0.sendMessage(ChatColor.RED + "No team with this name!");
		}

		return true;
	}

}
