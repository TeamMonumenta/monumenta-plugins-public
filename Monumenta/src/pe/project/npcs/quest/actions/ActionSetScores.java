package pe.project.npcs.quest.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import org.bukkit.entity.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import pe.project.Plugin;
import pe.project.utils.ScoreboardUtils;

public class ActionSetScores implements ActionBase {
	protected class SetScore {
		String mScoreboard;
		int mValue;

		SetScore(String scoreboard, int value) {
			mScoreboard = scoreboard;
			mValue = value;
		}

		void apply(Player player) {
			ScoreboardUtils.setScoreboardValue(player, mScoreboard, mValue);
		}
	}

	ArrayList<SetScore> mScoresToSet = new ArrayList<SetScore>();

	public ActionSetScores(JsonElement element) throws Exception {
		JsonArray array = element.getAsJsonArray();
		if (array == null) {
			throw new Exception("set_scores value is not an array!");
		}

		Iterator<JsonElement> iter = array.iterator();
		while (iter.hasNext()) {
			JsonElement entry = iter.next();

			Iterator<Entry<String, JsonElement>> scoreIter = entry.getAsJsonObject().entrySet().iterator();
			while (scoreIter.hasNext()) {
				Entry<String, JsonElement> scoreEntry = scoreIter.next();

				mScoresToSet.add(new SetScore(scoreEntry.getKey(), scoreEntry.getValue().getAsInt()));
			}
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player) {
		for (SetScore score : mScoresToSet) {
			score.apply(player);
		}
	}
}
