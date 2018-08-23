package com.playmonumenta.plugins.server.reset;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class DailyReset {
	public static void handle(Plugin plugin, Player player) {
		if (plugin.mServerProperties.getDailyResetEnabled()) {
			if (player != null) {
				//  Test to see if the player's Daily version is different than the servers.
				int dailyVersion = ScoreboardUtils.getScoreboardValue(player, "DailyVersion");
				if (dailyVersion != plugin.mDailyQuestVersion) {
					//  If so reset some scoreboards and message the player.
					ScoreboardUtils.setScoreboardValue(player, "DailyVersion", plugin.mDailyQuestVersion);

					ScoreboardUtils.setScoreboardValue(player, "DailyQuest", 0);

					if (ScoreboardUtils.getScoreboardValue(player, "TP_Farr") >= 1) {
						player.sendMessage(ChatColor.BOLD + "" + ChatColor.DARK_AQUA + "The king's bounty has changed! Perhaps you should seek out the Herald...");
					}

					/* Reset the player's access to the Patreon shrine (if applicable) */
					int Patreon = ScoreboardUtils.getScoreboardValue(player, "Patreon");
					int ShrinePower = (Patreon >= 20) ? 2 : ((Patreon >= 10) ? 1 : 0);
					ScoreboardUtils.setScoreboardValue(player, "ShrinePower", ShrinePower);

					if (ShrinePower >= 1) {
						player.sendMessage(ChatColor.BOLD + "" + ChatColor.DARK_AQUA + "Your ability to activate the Sierhaven shrine has been restored");
						if (ShrinePower == 1) {
							player.sendMessage(ChatColor.DARK_AQUA + "You can activate the shrine once each day");
						} else {
							player.sendMessage(ChatColor.DARK_AQUA + "You can activate the shrine twice (or two effects) each day");
						}
						player.sendMessage(ChatColor.DARK_AQUA + "Thank you for supporting the server!");
					}
				}
			}
		}
	}
}
