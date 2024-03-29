package com.playmonumenta.plugins.utils;

import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

public class ScoreboardUtils {
	public static OptionalInt getScoreboardValue(String scoreHolder, String objectiveName) {
		OptionalInt scoreValue = OptionalInt.empty();
		Objective objective = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(objectiveName);
		if (objective != null) {
			Score score = objective.getScore(scoreHolder);
			if (score.isScoreSet()) {
				scoreValue = OptionalInt.of(score.getScore());
			}
		}

		return scoreValue;
	}

	public static OptionalInt getScoreboardValue(Entity entity, String objectiveName) {
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

	public static void resetScoreboardValue(String entryName, String objectiveName) {
		Objective objective = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(objectiveName);
		if (objective != null) {
			Score score = objective.getScore(entryName);
			score.resetScore();
		}
	}

	public static void resetScoreboardValue(Entity entity, String objectiveName) {
		if (entity instanceof Player) {
			resetScoreboardValue(entity.getName(), objectiveName);
		} else {
			resetScoreboardValue(entity.getUniqueId().toString(), objectiveName);
		}
	}

	public static boolean toggleBinaryScoreboard(Player player, String scoreboard) {
		return toggleBinaryScoreboard(player, scoreboard, 0);
	}

	public static boolean toggleBinaryScoreboard(Player player, String scoreboard, int defaultValue) {
		boolean trueBefore = getScoreboardValue(player, scoreboard).orElse(defaultValue) == 1;
		if (trueBefore) {
			setScoreboardValue(player, scoreboard, 0);
		} else {
			setScoreboardValue(player, scoreboard, 1);
		}
		return !trueBefore;
	}

	public static boolean checkTag(
		Entity entity,
		String tag
	) {
		return entity.getScoreboardTags().contains(tag);
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

	public static void addEntityToTeam(Entity entity, String teamName, NamedTextColor color) {
		Team team = getExistingTeamOrCreate(teamName, color);

		if (entity instanceof Player player) {
			team.addEntry(player.getName());
		} else {
			team.addEntry(entity.getUniqueId().toString());
		}
	}

	public static void modifyTeamColor(String teamName, NamedTextColor color) {
		Team team = getExistingTeam(teamName);
		if (team != null) {
			team.color(color);
		}
	}

	public static void emptyTeam(String teamName) {
		Team team = getExistingTeam(teamName);
		if (team == null) {
			return;
		}
		Set<String> entries = new HashSet<>(team.getEntries());
		for (String entry : entries) {
			team.removeEntry(entry);
		}
	}

	public static boolean addScore(Entity entity, String objectiveName, int add) {
		OptionalInt scoreboardValue = getScoreboardValue(entity, objectiveName);
		if (scoreboardValue.isEmpty()) {
			return false;
		}
		setScoreboardValue(entity, objectiveName, scoreboardValue.getAsInt() + add);
		return true;
	}

	public static Objective createObjective(String name, Component displayName) {
		return Bukkit.getScoreboardManager().getMainScoreboard().registerNewObjective(name, Criteria.DUMMY, displayName);
	}

	public static Objective createObjectiveOnTempScoreboard(String name, Component displayName) {
		return Bukkit.getScoreboardManager().getNewScoreboard().registerNewObjective(name, Criteria.DUMMY, displayName);
	}

	public static void removeScoreboard(String name) {
		Objective objective = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(name);
		if (objective != null) {
			objective.unregister();
		}
	}
}
