package com.playmonumenta.plugins.rawcommands;

import java.util.LinkedHashMap;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;

public class UpdateApartments {
	public static void register() {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		CommandAPI.getInstance().register("updateapartments",
		                                  CommandPermission.fromString("monumenta.command.updateapartments"),
		                                  arguments,
		                                  (sender, args) -> {
											  run(sender);
		                                  }
		);
	}

	private static void run(CommandSender sender) throws CommandSyntaxException {
		Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		Objective aptObjective = scoreboard.getObjective("Apartment");
		Objective aptIdleObjective = scoreboard.getObjective("AptIdle");

		if (aptObjective == null) {
			CommandAPI.fail("Scoreboard 'Apartment' does not exist!");
		} else if (aptIdleObjective == null) {
			CommandAPI.fail("Scoreboard 'AptIdle' does not exist!");
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
	}
}
