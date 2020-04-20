package com.playmonumenta.plugins.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;

import com.playmonumenta.plugins.server.properties.ServerProperties;

public class ScoreboardUtils {

	private static final Map<String, String> DELVE_SHARD_SCOREBOARD_MAPPINGS = new HashMap<String, String>();

	static {
		DELVE_SHARD_SCOREBOARD_MAPPINGS.put("white", "Delve1Challenge");
		DELVE_SHARD_SCOREBOARD_MAPPINGS.put("orange", "Delve2Challenge");
		DELVE_SHARD_SCOREBOARD_MAPPINGS.put("magenta", "Delve3Challenge");
		DELVE_SHARD_SCOREBOARD_MAPPINGS.put("lightblue", "Delve4Challenge");
		DELVE_SHARD_SCOREBOARD_MAPPINGS.put("yellow", "Delve5Challenge");
		DELVE_SHARD_SCOREBOARD_MAPPINGS.put("lime", "Delve6Challenge");
		DELVE_SHARD_SCOREBOARD_MAPPINGS.put("pink", "Delve7Challenge");
		DELVE_SHARD_SCOREBOARD_MAPPINGS.put("gray", "Delve8Challenge");
		DELVE_SHARD_SCOREBOARD_MAPPINGS.put("lightgray", "Delve9Challenge");
		DELVE_SHARD_SCOREBOARD_MAPPINGS.put("cyan", "Delve10Challenge");
		DELVE_SHARD_SCOREBOARD_MAPPINGS.put("purple", "Delve11Challenge");
		DELVE_SHARD_SCOREBOARD_MAPPINGS.put("dev1", "DelveChallenge");
		DELVE_SHARD_SCOREBOARD_MAPPINGS.put("dev2", "DelveChallenge");
		DELVE_SHARD_SCOREBOARD_MAPPINGS.put("mobs", "DelveChallenge");
	}

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

	public static boolean isDelveChallengeActive(Player player, int challengeScore) {
		if (player == null) {
			return false;
		}

		String scoreboard = DELVE_SHARD_SCOREBOARD_MAPPINGS.get(ServerProperties.getShardName());

		if (scoreboard != null) {
			return ScoreboardUtils.getScoreboardValue(player, scoreboard) == challengeScore;
		}

		return false;
	}

}
