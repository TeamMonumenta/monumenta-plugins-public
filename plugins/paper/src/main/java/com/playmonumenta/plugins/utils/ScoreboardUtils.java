package com.playmonumenta.plugins.utils;

import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;

public class ScoreboardUtils {
	public static Optional<Integer> getScoreboardValue(String scoreHolder, String objectiveName) {
		Optional<Integer> scoreValue = Optional.empty();
		Objective objective = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(objectiveName);

		if (objective != null) {
			Score score = objective.getScore(scoreHolder);
			if (score.isScoreSet()) {
				scoreValue = Optional.of(score.getScore());
			}
		}

		return scoreValue;
	}

	public static Optional<Integer> getScoreboardValue(Entity entity, String objectiveName) {
		if (entity instanceof Player) {
			return getScoreboardValue(entity.getName(), objectiveName);
		} else {
			return getScoreboardValue(entity.getUniqueId().toString(), objectiveName);
		}
	}

	public static void setScoreboardValue(String entryName, String objectiveName, int value) {
		Objective objective = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(objectiveName);
		if (objective != null) {
			Score score = objective.getScore(entryName);
			score.setScore(value);
		}
	}

	public static void setScoreboardValue(Entity entity, String objectiveName, int value) {
		if (entity instanceof Player) {
			setScoreboardValue(entity.getName(), objectiveName, value);
		} else {
			setScoreboardValue(entity.getUniqueId().toString(), objectiveName, value);
		}
	}

	public static boolean checkTag(
		Player player,
		String tag
	) {
		return player.getScoreboardTags().contains(tag);
	}
}
