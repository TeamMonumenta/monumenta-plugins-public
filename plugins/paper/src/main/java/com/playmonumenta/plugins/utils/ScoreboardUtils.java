package com.playmonumenta.plugins.utils;

import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;

public class ScoreboardUtils {
	/**
	 * Get scoreboard value for player.
	 *
	 * @param player          the player object
	 * @param scoreboardValue the objective name
	 * @return the objective value associated with the player
	 */
	public static int getScoreboardValue(Player player, String scoreboardValue) {
		Objective objective = player.getScoreboard().getObjective(scoreboardValue);

		if (objective != null) {
			return objective.getScore(player.getName()).getScore();
		}

		return 0;
	}

	/**
	 * Get scoreboard value for player.
	 *
	 * @param playerName      the player name
	 * @param scoreboardValue the object name
	 * @return the objective value associated with the player
	 */
	public static Optional<Integer> getScoreboardValue(String playerName, String scoreboardValue) {
		Optional<Integer> scoreValue = Optional.empty();
		Objective objective = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(scoreboardValue);

		if (objective != null) {
			Score score = objective.getScore(playerName);
			if (score != null) {
				scoreValue = Optional.of(score.getScore());
			}
		}

		return scoreValue;
	}

	public static void setScoreboardValue(String playerName, String scoreboardValue, int value) {
		Objective objective = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(scoreboardValue);
		if (objective != null) {
			Score score = objective.getScore(playerName);
			score.setScore(value);
		}
	}

	public static void setScoreboardValue(Player player, String scoreboardValue, int value) {
		Objective objective = player.getScoreboard().getObjective(scoreboardValue);
		if (objective != null) {
			Score score = objective.getScore(player.getName());
			score.setScore(value);
		}
	}
}
