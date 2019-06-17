package com.playmonumenta.plugins.server.reset;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class DailyReset {
	private static BukkitRunnable mRunnable = null;
	public static void startTimer(Plugin plugin) {
		if (mRunnable == null || mRunnable.isCancelled()) {
			new BukkitRunnable() {
				int mCurVers = getDailyVersion();

				@Override
				public void run() {
					int newVers = getDailyVersion();
					if (newVers != mCurVers) {
						for (Player player : plugin.getServer().getOnlinePlayers()) {
							handle(plugin, player);
						}

						mCurVers = newVers;
					}
				}
			}.runTaskTimer(plugin, 0, 1200);
		}
	}

	private static int getDailyVersion() {
		return (int)((System.currentTimeMillis()-25200000) / 86400000);
	}

	public static void handle(Plugin plugin, Player player) {
		if (plugin.mServerProperties.getDailyResetEnabled() && player != null) {
			//  Test to see if the player's Daily version is different than the servers.
			int dailyVersion = ScoreboardUtils.getScoreboardValue(player, "DailyVersion");
			if (dailyVersion != getDailyVersion()) {
				//  If so reset some scoreboards and message the player.
				ScoreboardUtils.setScoreboardValue(player, "DailyVersion", getDailyVersion());

				ScoreboardUtils.setScoreboardValue(player, "DailyQuest", 0);
				ScoreboardUtils.setScoreboardValue(player, "Daily2Quest", 0);

				if (ScoreboardUtils.getScoreboardValue(player, "TP_Farr") >= 1) {
					player.sendMessage(ChatColor.BOLD + "" + ChatColor.DARK_AQUA + "The king's bounty has changed! Perhaps you should seek out the Herald...");
				}
				if (ScoreboardUtils.getScoreboardValue(player, "Quest101") >= 13) {
					player.sendMessage(ChatColor.BOLD + "" + ChatColor.DARK_AQUA + "The chains of fate have realigned. Perhaps you should ask the Seer for new wisdom...");
				}

				// Remove the tag that prevents players from beating Azacor more than once per day
				player.removeScoreboardTag("am_antiartifact");

				// Remove the tag that prevents players from beating Kaul more than once per day
				player.removeScoreboardTag("kaul_daily_artifact");

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
