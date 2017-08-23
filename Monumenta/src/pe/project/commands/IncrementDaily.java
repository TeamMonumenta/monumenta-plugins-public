package pe.project.commands;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import pe.project.Main;
import pe.project.utils.ScoreboardUtils;

public class IncrementDaily implements CommandExecutor {
	Main mPlugin;
	
	public IncrementDaily(Main plugin) {
		mPlugin = plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		//	Increment the servers Daily version.
		mPlugin.incrementDailyVersion();
		
		//	Loop through all online players, reset their scoreboards and message them about the Daily reset.
		List<Player> players = Bukkit.getWorlds().get(0).getPlayers();
		for (Player player : players) {
			if (player.isOnline()) {
				ScoreboardUtils.setScoreboardValue(player, "DailyVersion", mPlugin.mDailyQuestVersion);
				
				ScoreboardUtils.setScoreboardValue(player, "DailyQuest", 0);
				
				if (ScoreboardUtils.getScoreboardValue(player, "Farr") >= 1) {
					player.sendMessage(ChatColor.BOLD + "" + ChatColor.DARK_AQUA + "The king's bounty has changed! Perhaps you should seek out the Herald...");
				}
			}
		}
		
		return true;
	}
}
