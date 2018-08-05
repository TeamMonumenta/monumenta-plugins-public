package com.playmonumenta.plugins.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class UpdateApartments implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length != 0) {
			sender.sendMessage(ChatColor.RED + "Invalid number of parameters!");
			return false;
		}

		Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		Objective aptObjective = scoreboard.getObjective("Apartment");
		Objective aptIdleObjective = scoreboard.getObjective("AptIdle");

		if (aptObjective == null) {
			sender.sendMessage(ChatColor.RED + "Scoreboard 'Apartment' does not exist!");
			return false;
		} else if (aptIdleObjective == null) {
			sender.sendMessage(ChatColor.RED + "Scoreboard 'AptIdle' does not exist!");
			return false;
		}

		for (String entry : scoreboard.getEntries()) {
			int aptScore = aptObjective.getScore(entry).getScore();
			int aptIdleScore = aptIdleObjective.getScore(entry).getScore();

			/* If the player has no Apartment but has an AptIdle score, clear it */
			if (aptScore <= 0 && aptIdleScore > 0) {
				aptIdleScore = 0;
				aptIdleObjective.getScore(entry).setScore(aptIdleScore);
			}

			/* If the player has an AptIdle score, decrement it */
			if (aptIdleScore > 0) {
				aptIdleScore--;
				aptIdleObjective.getScore(entry).setScore(aptIdleScore);
			}

			/* If the player has no AptIdle score, clear Apartment score */
			if (aptIdleScore <= 0 && aptScore > 0) {
				aptScore = 0;
				aptObjective.getScore(entry).setScore(aptScore);
			}
		}

		return true;
	}
}
