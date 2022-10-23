package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.server.properties.ServerProperties;
import java.util.Optional;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Team;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ScoreboardUtils {
	public static Optional<Integer> getScoreboardValue(String scoreHolder, String objectiveName) {
		Optional<Integer> scoreValue = Optional.empty();
		Objective objective = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(objectiveName);
		// Fast track applies
		if (ServerProperties.getAbilityEnhancementsEnabled() && !ServerProperties.getShardName().contains("plots")) {
			if (objectiveName.equals(AbilityUtils.TOTAL_LEVEL)) {
				return Optional.of(AbilityUtils.MAX_SKILL_POINTS);
			} else if (objectiveName.equals(AbilityUtils.TOTAL_SPEC)) {
				return Optional.of(AbilityUtils.MAX_SPEC_POINTS);
			}
		}
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

	/**
	 * "Toggles" a tag between being present and being absent
	 *
	 * @return Whether the tag is present after toggling it
	 */
	public static boolean toggleTag(Player player, String tag) {
		boolean removed = player.getScoreboardTags().remove(tag);
		if (!removed) {
			player.getScoreboardTags().add(tag);
		}
		return !removed;
	}

	public static Team createTeam(String teamName) {
		return Bukkit.getScoreboardManager().getMainScoreboard().registerNewTeam(teamName);
	}

	public static Team createTeam(String teamName, NamedTextColor color) {
		Team team = createTeam(teamName);
		team.color(color);
		return team;
	}

	public static @Nullable Team getExistingTeam(String teamName) {
		return Bukkit.getScoreboardManager().getMainScoreboard().getTeam(teamName);
	}

	public static Team getExistingTeamOrCreate(String teamName) {
		Team team = getExistingTeam(teamName);
		if (team == null) {
			team = createTeam(teamName);
		}
		return team;
	}

	public static Team getExistingTeamOrCreate(String teamName, NamedTextColor color) {
		Team team = getExistingTeam(teamName);
		if (team == null) {
			team = createTeam(teamName, color);
		} else if (team.color() != color) {
			team.color(color);
		}
		return team;
	}

	public static @Nullable Team getEntityTeam(Entity entity) {
		return Bukkit.getScoreboardManager().getMainScoreboard().getEntityTeam(entity);
	}

	public static void addEntityToTeam(Entity entity, String teamName) {
		Team team = getExistingTeamOrCreate(teamName);

		if (entity instanceof Player player) {
			team.addEntry(player.getName());
		} else {
			team.addEntry(entity.getUniqueId().toString());
		}
	}

	public static int getScoreOrDefault(Entity entity, String objectiveName, int defaultResult) {
		if (!getScoreboardValue(entity, objectiveName).isPresent()) {
			return defaultResult;
		}
		return getScoreboardValue(entity, objectiveName).get();
	}
}
