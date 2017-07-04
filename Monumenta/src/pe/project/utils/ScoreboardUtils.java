package pe.project.utils;

import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

public class ScoreboardUtils {
	public static int getScoreboardValue(Player player, String scoreboardValue) {
		Objective objective = player.getScoreboard().getObjective(scoreboardValue);
		if (objective != null) {
			return objective.getScore(player.getName()).getScore();
		}
		
		return 0;
	}
	
	//	Works with dummy players.
	public static int getScoreboardValue(String playerName, String scoreboardValue) {
		Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		Set<Objective> objectives = scoreboard.getObjectives();
		
		for (Objective objective : objectives) {
			if (objective.getName().equals(scoreboardValue)) {
				Score score = objective.getScore(playerName);
				if (score != null) {
					return score.getScore();
				}
			}
		}
		
		return -1;
	}
	
	public static void setScoreboardValue(Player player, String scoreboardValue, int value) {
		Objective objective = player.getScoreboard().getObjective(scoreboardValue);
		if (objective != null) {
			Score score = objective.getScore(player.getName());
			score.setScore(value);
		}
	}
}
