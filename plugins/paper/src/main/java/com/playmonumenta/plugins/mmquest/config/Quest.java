package com.playmonumenta.plugins.mmquest.config;

import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.Map;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;


public class Quest {
	public String mQuestName = "";
	public String mQuestNumber = "";
	public String mQuestDescription = "";
	public Integer[] mQuestCompleteScores;
	public String[] mQuestReqs;
	public Map<String, String> mQuestValues;


	//Constructor:
	public Quest(String questName, String questNumber, String questDescription,
	             Integer[] questCompleteScores, String[] questReqs, Map<String, String> questValues) {
		this.mQuestName = questName;
		this.mQuestNumber = questNumber;
		this.mQuestDescription = questDescription;
		this.mQuestCompleteScores = questCompleteScores;
		this.mQuestReqs = questReqs;
		this.mQuestValues = questValues;
	}

	public boolean checkQuestCompletion(Player player) {
		Scoreboard scoreboard = player.getScoreboard();
		Objective objective = scoreboard.getObjective(mQuestNumber);
		if (objective == null) {
			return false;
		}

		Integer playerScore = objective.getScore(player.getName()).getScore();
		boolean matchesAny = false;
		// If empty array, just return false:
		if (mQuestCompleteScores.length == 0) {
			return false;
		}
		// Keep track of maximum:
		int max = Integer.MIN_VALUE;
		for (Integer score : mQuestCompleteScores) {
			if (Objects.equals(score, playerScore)) {
				matchesAny = true;
				break;
			}
			if (score > max) {
				max = score;
			}
		}
		// Test if player's score is higher than the max:
		if (playerScore > max) {
			return true;
		}
		return matchesAny;
	}

	public int getPlayerScore(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, mQuestNumber).orElse(-1);
	}
}
