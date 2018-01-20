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
		// This should be an enum, but idk how to set those up. -Nick
		int mOperation;
		private static final int SET_EXACT = 1;
		private static final int SET_COPY = 2;

		String mScoreboard;
		String mSourceScore;
		int mValue;

		SetScore(String scoreboard, int value) {
			mScoreboard = scoreboard;
			mValue = value;
			mOperation = SET_EXACT;
		}

		SetScore(String destinationScore, String sourceScore) {
			mScoreboard = destinationScore;
			mSourceScore = sourceScore;
			mOperation = SET_COPY;
		}

		void apply(Player player) {
			switch (mOperation) {
			  case SET_EXACT:
			    ScoreboardUtils.setScoreboardValue(player, mScoreboard, mValue);
			    break;
			  case SET_COPY:
			    int temp_score = ScoreboardUtils.getScoreboardValue(player, mSourceScore);
			    ScoreboardUtils.setScoreboardValue(player, mScoreboard, temp_score);
			    break;
			}
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

				String key = scoreEntry.getKey();

				String valueAsString = scoreEntry.getValue().getAsString();
				if (valueAsString != null) {
				  mScoresToSet.add(new SetScore(key, valueAsString));
				} else {
				  int valueAsInt = scoreEntry.getValue().getAsInt();
			    mScoresToSet.add(new SetScore(key, valueAsInt));
			  }
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
