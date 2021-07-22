package com.playmonumenta.plugins.utils;

import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;



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

	public static boolean checkTag(
		@NotNull Player player,
		@NotNull String tag
	) {
		return player.getScoreboardTags().contains(tag);
	}

	//TODO combine implementation with getScoreboardValue() above
	/*
	 * Gets the value of the specified objective for the specified player.
	 * If the player has no value set for that objective,
	 * the specified defaultValue is returned instead.
	 */
	public static int getValue(
		@NotNull Player player,
		@NotNull String objectiveName,
		int defaultValue
	) {
		@Nullable Objective objective = player.getScoreboard().getObjective(objectiveName);
		if (objective != null) {
			@NotNull Score score = objective.getScore(player.getName());
			if (score.isScoreSet()) {
				return score.getScore();
			}
		}

		return defaultValue;
	}
}
