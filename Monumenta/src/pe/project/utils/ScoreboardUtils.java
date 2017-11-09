package pe.project.utils;

import java.util.Iterator;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

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
		for (String tag : player.getScoreboardTags()) {
			tagArray.add(tag);
		}
		returnData.add("tags", tagArray);


		/*
		 * "team" : [
		 *  "name" : "theName",
		 *  "displayName" : "theDisplayName",
		 *  "prefix" : "thePrefix",
		 *  "suffix" : "theSuffix",
		 *  "color" : "theColor",
		 *  "members" : [
		 *   "member1",
		 *   "member2",
		 *  ]
		 * ]
		 */
		Team team = scoreboard.getEntryTeam(player.getName());
		if (team != null) {
			JsonObject teamObject = new JsonObject();

			String name = team.getName();
			if (name == null) name = "";
			teamObject.addProperty("name", name);

			String displayName = team.getDisplayName();
			if (displayName == null) displayName = "";
			teamObject.addProperty("displayName", displayName);

			String prefix = team.getPrefix();
			if (prefix == null) prefix = "";
			teamObject.addProperty("prefix", prefix);

			String suffix = team.getSuffix();
			if (suffix == null) suffix = "";
			teamObject.addProperty("suffix", suffix);

			ChatColor color = team.getColor();
			if (color == null) color = ChatColor.WHITE;
			teamObject.addProperty("color", color.name());

			JsonArray teamMembers = new JsonArray();
			for (String entry : team.getEntries()) {
				teamMembers.add(entry);
			}
			teamObject.add("members", teamMembers);

			// Add this whole collection to the player data
			returnData.add("team", teamObject);
		}

		return returnData;
	}

	public static void loadFromJsonObject(Player player, JsonObject object) throws Exception {
		Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

		// Load scoreboards first
		Iterator<JsonElement> scoreIter = object.get("scores").getAsJsonArray().iterator();
		while (scoreIter.hasNext()) {
			JsonObject scoreboardObject = scoreIter.next().getAsJsonObject();

			String name = scoreboardObject.get("name").getAsString();
			int scoreVal = scoreboardObject.get("score").getAsInt();

			Objective objective = scoreboard.getObjective(name);
			if (objective == null) {
				objective = scoreboard.registerNewObjective(name, "dummy");
			}

			Score score = objective.getScore(player.getName());
			score.setScore(scoreVal);
		}

		// Remove player's tags
		player.getScoreboardTags().clear();

		// Add player tags from JSON
		Iterator<JsonElement> tagIter = object.get("tags").getAsJsonArray().iterator();
		while (tagIter.hasNext()) {
			player.addScoreboardTag(tagIter.next().getAsString());
		}

		Team currentTeam = scoreboard.getEntryTeam(player.getName());

		if (object.has("team")) {
			JsonObject teamObject = object.get("team").getAsJsonObject();

			String name = teamObject.get("name").getAsString();
			String displayName = teamObject.get("displayName").getAsString();
			String prefix = teamObject.get("prefix").getAsString();
			String suffix = teamObject.get("suffix").getAsString();
			String color = teamObject.get("color").getAsString();

			Team newTeam = null;
			if (currentTeam != null) {
				if (currentTeam.getName().equals(name)) {
					// Already on this team
					newTeam = currentTeam;
				} else {
					// Joined to a different team - need to leave it
					currentTeam.removeEntry(player.getName());

					// If the team is empty, remove it
					if (currentTeam.getSize() <= 0) {
						currentTeam.unregister();
					}
				}
			}

			// If newTeam still null, need to join to it
			if (newTeam == null) {
				// Look up the right team
				newTeam = scoreboard.getTeam(name);

				// If newTeam *still* null, this team doesn't exist
				if (newTeam == null) {
					newTeam = scoreboard.registerNewTeam(name);
				}

				// Join player to the team
				newTeam.addEntry(player.getName());
			}

			newTeam.setDisplayName(displayName);
			newTeam.setPrefix(prefix);
			newTeam.setSuffix(suffix);
			newTeam.setColor(ChatColor.valueOf(color));

			// Note - team member list not used here
		} else if (currentTeam != null){
			/*
			 * TODO:
			 *
			 * Maybe we want this eventually, but not yet. It would clear the teams for
			 * players who aren't on region_1 when it is added, which is bad
			 */
			//  // If no team info was sent but player on a team remove player from team
			//  currentTeam.removeEntry(player.getName());

			//  // If the team is empty, remove it
			//  if (currentTeam.getSize() <= 0) {
			//  	currentTeam.unregister();
			//  }
		}
	}
}
