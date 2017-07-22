package pe.project.utils;

import java.util.Iterator;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
	
	public static JsonArray getAsJsonObject(Player player) {
		JsonArray scoreboardArray = new JsonArray();
		
		Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		Set<Objective> objectives = scoreboard.getObjectives();
		
		for (Objective objective : objectives) {
			Score score = objective.getScore(player.getName());
			if (score != null) {
				JsonObject scoreboardInfo = new JsonObject();
				
				scoreboardInfo.addProperty("name", objective.getName());
				scoreboardInfo.addProperty("score", score.getScore());
				
				scoreboardArray.add(scoreboardInfo);
			}
		}
		
		return scoreboardArray;
	}
	
	public static void loadFromJsonObject(Player player, JsonArray object) {
		Iterator<JsonElement> iter = object.iterator();
		while (iter.hasNext()) {
			JsonObject scoreboardObject = iter.next().getAsJsonObject();
			
			String name = scoreboardObject.get("name").getAsString();
			int scoreVal = scoreboardObject.get("score").getAsInt();
			
			Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
			Objective objective = scoreboard.getObjective(name);
			if (objective == null) {
				objective = scoreboard.registerNewObjective(name, "dummy");
			}
			
			Score score = objective.getScore(player.getName());
			score.setScore(scoreVal);
		}
	}
}
