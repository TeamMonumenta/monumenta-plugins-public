package pe.project.commands;

import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

public class TransferScores implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		if (arg0 instanceof Player) {
			Player playerSender = (Player)arg0;

			if (arg3.length < 1) {
	    		arg0.sendMessage(ChatColor.RED + "Too few parameters!");
	    		arg0.sendMessage(ChatColor.RED + "Usage: " + arg1.getUsage());
	    		return false;
	    	} else if (arg3.length > 2) {
	    		arg0.sendMessage(ChatColor.RED + "Too many parameters!");
	    		arg0.sendMessage(ChatColor.RED + "Usage: " + arg1.getUsage());
	    		return false;
	    	}

			List<Player> players = Bukkit.getWorlds().get(0).getPlayers();
			Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
			Set<Objective> objectives = scoreboard.getObjectives();

			String from = arg3[0];
			String to = arg3[1];

			boolean fromPlayerExist = scoreboard.getEntries().contains(from);
			boolean toPlayerExist = scoreboard.getEntries().contains(to);


			if (!fromPlayerExist) {
				playerSender.sendMessage(ChatColor.RED + "Old player scoreboard does not exist. Have they ever been on the server or was the name typed incorrectly?");
				return false;
			}

			if (!toPlayerExist) {
				playerSender.sendMessage(ChatColor.RED + "New player scoreboard does not exist. Have they ever been on the server or was the name typed incorrectly?");
				return false;
			}

			//	Additionally to prevent any potential fuck ups by people using this....we want to make sure the from player is offline
			//	and to too player is online...
			boolean fromPlayerOffline = true;
			boolean toPlayerOnline = false;

			for (Player player : players) {
				if (fromPlayerOffline == true && player.getName().contains(from)) {
					fromPlayerOffline = false;
				} else if (toPlayerOnline == false && player.getName().contains(to)) {
					toPlayerOnline = true;
				}

				if (fromPlayerOffline == false && toPlayerOnline == true) {
					break;
				}
			}

			if (!fromPlayerOffline || !toPlayerOnline) {
				playerSender.sendMessage(ChatColor.RED + "Cannot only transfer scores from an offline player to an online player. (To prevent accidently breaking people)");
				return false;
			}

			//	Transfer Scoreboards from the old name to the new name!
			for (Objective objective : objectives) {
				Score toScore = objective.getScore(to);
				Score fromScore = objective.getScore(from);
				if (toScore != null && fromScore != null) {
					toScore.setScore(fromScore.getScore());
				}
			}

			playerSender.sendMessage(ChatColor.GREEN + "Successfully transfered scoreboard values between the old player and the new!");
		}

		return true;
	}
}
