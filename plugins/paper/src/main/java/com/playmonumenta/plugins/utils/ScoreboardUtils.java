package com.playmonumenta.plugins.utils;

import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScoreboardUtils {
	public static @NotNull Optional<Integer> getScoreboardValue(@NotNull String scoreHolder, @NotNull String objectiveName) {
		@NotNull Optional<Integer> scoreValue = Optional.empty();
		@Nullable Objective objective = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(objectiveName);

		if (objective != null) {
			@NotNull Score score = objective.getScore(scoreHolder);
			if (score.isScoreSet()) {
				scoreValue = Optional.of(score.getScore());
			}
		}

		return scoreValue;
	}

	public static @NotNull Optional<Integer> getScoreboardValue(@NotNull Entity entity, @NotNull String objectiveName) {
		return getScoreboardValue(entity.getUniqueId().toString(), objectiveName);
	}

	public static @NotNull Optional<Integer> getScoreboardValue(@NotNull Player player, @NotNull String objectiveName) {
		return getScoreboardValue(player.getName(), objectiveName);
	}

	public static void setScoreboardValue(@NotNull String entryName, @NotNull String objectiveName, int value) {
		@Nullable Objective objective = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(objectiveName);
		if (objective != null) {
			@NotNull Score score = objective.getScore(entryName);
			score.setScore(value);
		}
	}

	public static void setScoreboardValue(@NotNull Entity entity, @NotNull String objectiveName, int value) {
		setScoreboardValue(entity.getUniqueId().toString(), objectiveName, value);
	}

	public static void setScoreboardValue(@NotNull Player player, @NotNull String objectiveName, int value) {
		setScoreboardValue(player.getName(), objectiveName, value);
	}

	public static boolean checkTag(
		@NotNull Player player,
		@NotNull String tag
	) {
		return player.getScoreboardTags().contains(tag);
	}
}
