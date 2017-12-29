package pe.project.server.reset;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import pe.project.Plugin;
import pe.project.utils.ScoreboardUtils;

public class DailyReset {
	public static void handle(Plugin plugin, Player player) {
		if (plugin.mServerProperties.getDailyResetEnabled()) {
			if (player != null) {
				//	Test to see if the player's Daily version is different than the servers.
				int dailyVersion = ScoreboardUtils.getScoreboardValue(player, "DailyVersion");
				if (dailyVersion != plugin.mDailyQuestVersion) {
					//	If so reset some scoreboards and message the player.
					ScoreboardUtils.setScoreboardValue(player, "DailyVersion", plugin.mDailyQuestVersion);

					ScoreboardUtils.setScoreboardValue(player, "DailyQuest", 0);

					if (ScoreboardUtils.getScoreboardValue(player, "TP_Farr") >= 1) {
						player.sendMessage(ChatColor.BOLD + "" + ChatColor.DARK_AQUA + "The king's bounty has changed! Perhaps you should seek out the Herald...");
					}
				}
			}
		}
	}
}
