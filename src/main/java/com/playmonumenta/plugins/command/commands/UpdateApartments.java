package com.playmonumenta.plugins.command.commands;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import com.playmonumenta.plugins.command.AbstractCommand;
import com.playmonumenta.plugins.command.CommandContext;

import net.sourceforge.argparse4j.inf.ArgumentParser;

public class UpdateApartments extends AbstractCommand {

	public UpdateApartments(Plugin plugin) {
		super(
		    "updateApartments",
		    "Updates player apartment scores",
		    plugin
		);
	}

	@Override
	protected void configure(ArgumentParser parser) {
	}

	@Override
	protected boolean run(CommandContext context) {
		Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		Objective aptObjective = scoreboard.getObjective("Apartment");
		Objective aptIdleObjective = scoreboard.getObjective("AptIdle");

		if (aptObjective == null) {
			sendErrorMessage(context, "Scoreboard 'Apartment' does not exist!");
			return false;
		} else if (aptIdleObjective == null) {
			sendErrorMessage(context, "Scoreboard 'AptIdle' does not exist!");
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
