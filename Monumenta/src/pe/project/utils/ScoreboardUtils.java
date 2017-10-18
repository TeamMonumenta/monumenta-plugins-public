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

	public static JsonObject getAsJsonObject(Player player) {
		// returned data contains an array of scoreboard key/value pairs and an array of tags
		JsonObject returnData = new JsonObject();

		// Scoreboards
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

		returnData.add("scores", scoreboardArray);

		// Tags
		JsonArray tagArray = new JsonArray();
		Set<String> playerTags = player.getScoreboardTags();
		for (String tag : playerTags) {
			tagArray.add(tag);
		}

		returnData.add("tags", tagArray);

		return returnData;
	}

	public static void loadFromJsonObject(Player player, JsonObject object) {
		// Load scoreboards first
		Iterator<JsonElement> scoreIter = object.get("scores").getAsJsonArray().iterator();
		while (scoreIter.hasNext()) {
			JsonObject scoreboardObject = scoreIter.next().getAsJsonObject();

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

		// Remove player's tags
		Set<String> playerTags = player.getScoreboardTags();
		for (String tag : playerTags) {
			player.removeScoreboardTag(tag);
		}

		// Add player tags from JSON
		Iterator<JsonElement> tagIter = object.get("tags").getAsJsonArray().iterator();
		while (tagIter.hasNext()) {
			player.addScoreboardTag(tagIter.next().getAsString());
		}
	}
}
