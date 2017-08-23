package pe.project.server.reset;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import pe.project.Main;
import pe.project.utils.ScoreboardUtils;

public class DailyReset {
	public static void handle(Main plugin, Player player) {
		if (player != null) {
			new BukkitRunnable() {
				Integer tick = 0;
				public void run() {
					if (++tick == 20) {
						//	Test to see if the logging in player's Daily version is different than the servers.
						int dailyQuest = ScoreboardUtils.getScoreboardValue(player, "DailyQuest");
						if (dailyQuest != plugin.mDailyQuestVersion) {
							//	If so reset some scoreboards and message the player.
							ScoreboardUtils.setScoreboardValue(player, "DailyVersion", plugin.mDailyQuestVersion);
							
							ScoreboardUtils.setScoreboardValue(player, "DailyQuest", 0);
							
							if (ScoreboardUtils.getScoreboardValue(player, "Farr") >= 1) {
								player.sendMessage(ChatColor.BOLD + "" + ChatColor.DARK_AQUA + "The king's bounty has changed! Perhaps you should seek out the Herald...");
							}
						}
						
						this.cancel();
					}
				}
			}.runTaskTimer(plugin, 0, 1);
		}
	}
}
