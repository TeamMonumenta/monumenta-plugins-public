package pe.project.npcs.quest.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.bukkit.entity.Player;

import com.google.gson.JsonElement;

import pe.project.Plugin;
import pe.project.utils.ScoreboardUtils;

public class ActionSetScore implements ActionBase {
	protected class SetScore {
		// This should be an enum, but idk how to set those up. -Nick
		int mOperation;
		private static final int SET_EXACT = 1;
		private static final int SET_COPY = 2;
		private static final int SET_RANDOM = 3;

		String mSourceScore;
		int mValue;
		int mValueRange; // max - min + 1
		private Random mRandom = new Random();

		SetScore(int value) {
			mValue = value;
			mOperation = SET_EXACT;
		}

		SetScore(String sourceScore) {
			mSourceScore = sourceScore;
			mOperation = SET_COPY;
		}

		SetScore(int minValue, int maxValue) {
			mValue = minValue;
			mValueRange = maxValue - minValue + 1;
			mOperation = SET_RANDOM;
		}

		void apply(Player player, String targetScore) {
			int temp_score;
			switch (mOperation) {
			case SET_EXACT:
				ScoreboardUtils.setScoreboardValue(player, targetScore, mValue);
				break;
			case SET_COPY:
				temp_score = ScoreboardUtils.getScoreboardValue(player, mSourceScore);
				ScoreboardUtils.setScoreboardValue(player, targetScore, temp_score);
				break;
			case SET_RANDOM:
				temp_score = mValue + mRandom.nextInt(mValueRange);
				ScoreboardUtils.setScoreboardValue(player, targetScore, temp_score);
				break;
			}
		}
	}

	String mScoreName;
	SetScore mSetScore;

	public ActionSetScore(String scoreName, JsonElement value) throws Exception {
		mScoreName = scoreName;

		if (value.isJsonPrimitive()) {
			//	Single value

			// First try to parse the item as an integer
			try {
				int valueAsInt = value.getAsInt();
				mSetScore = new SetScore(valueAsInt);
			} catch (Exception e) {
				// If that failed, try a string instead
				String valueAsString = value.getAsString();
				if (valueAsString != null) {
					mSetScore = new SetScore(valueAsString);
				} else {
					throw new Exception("set_score value for scoreboard '" + mScoreName + "' is neither an integer nor a string!");
				}
			}
		} else {
			// Range of values
			Integer imin = Integer.MIN_VALUE;
			Integer imax = Integer.MAX_VALUE;

			Set<Entry<String, JsonElement>> subentries = value.getAsJsonObject().entrySet();
			for (Entry<String, JsonElement> subent : subentries) {
				String rangeKey = subent.getKey();

				switch(rangeKey) {
				case "min":
					imin = subent.getValue().getAsInt();
					break;
				case "max":
					imax = subent.getValue().getAsInt();
					break;
				default:
					throw new Exception("Unknown check_score value: '" + rangeKey + "'");
				}
			}

			if (imin == Integer.MIN_VALUE && imax == Integer.MAX_VALUE) {
				throw new Exception("Bogus check_score object with no min or max");
			}

			mSetScore = new SetScore(imin, imax);
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player) {
		mSetScore.apply(player, mScoreName);
	}
}
