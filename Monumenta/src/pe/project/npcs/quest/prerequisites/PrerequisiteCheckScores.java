package pe.project.npcs.quest.prerequisites;

import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.entity.Player;

import com.google.gson.JsonElement;

import pe.project.utils.ScoreboardUtils;

public class PrerequisiteCheckScores implements PrerequisiteBase {
	protected class CheckScore {
		// This should be an enum, but idk how to set those up. -Nick
		int mOperation;
		private static final int CHECK_EXACT = 1;
		private static final int CHECK_OTHER = 2;
		private static final int CHECK_RANGE = 3;

		String mOtherScore;
		int mMin;
		int mMax;

		CheckScore(int value) {
			mMin = value;
			mOperation = CHECK_EXACT;
		}

		CheckScore(String value) {
			mOtherScore = value;
			mOperation = CHECK_OTHER;
		}

		CheckScore(int min, int max) {
			mMin = min;
			mMax = max;
			mOperation = CHECK_RANGE;
		}

		boolean check(Player player, String scoreName) {
			int value = ScoreboardUtils.getScoreboardValue(player, scoreName);
			switch (mOperation) {
			case CHECK_EXACT:
				return value == mMin;
			case CHECK_OTHER:
				mMin = ScoreboardUtils.getScoreboardValue(player, mOtherScore);
				return value == mMin;
			case CHECK_RANGE:
				return value >= mMin && value <= mMax;
			default:
				return false;
			}
		}
	}

	String mScoreName;
	CheckScore mCheckScore;

	public PrerequisiteCheckScores(String scoreName, JsonElement value) throws Exception {
		mScoreName = scoreName;

		if (value.isJsonPrimitive()) {
			//	Single value

			// First try to parse the item as an integer
			try {
				int valueAsInt = value.getAsInt();
				mCheckScore = new CheckScore(valueAsInt);
			} catch (Exception e) {
				// If that failed, try a string instead
				String valueAsString = value.getAsString();
				if (valueAsString != null) {
					mCheckScore = new CheckScore(valueAsString);
				} else {
					throw new Exception("check_score value for scoreboard '" + mScoreName + "' is neither an integer nor a string!");
				}
			}
		} else {
			// Range of values
			Integer imin = Integer.MIN_VALUE;
			Integer imax = Integer.MAX_VALUE;

			Set<Entry<String, JsonElement>> subentries = value.getAsJsonObject().entrySet();
			for (Entry<String, JsonElement> subent : subentries) {
				String rangeKey = subent.getKey();

				if (rangeKey.equals("min")) {
					imin = subent.getValue().getAsInt();
				} else if (rangeKey.equals("max")) {
					imax = subent.getValue().getAsInt();
				} else {
					throw new Exception("Unknown check_score value: '" + rangeKey + "'");
				}
			}

			if (imin == Integer.MIN_VALUE && imax == Integer.MAX_VALUE) {
				throw new Exception("Bogus check_score object with no min or max");
			}

			mCheckScore = new CheckScore(imin, imax);
		}
	}

	@Override
	public boolean prerequisiteMet(Player player) {
		return mCheckScore.check(player, mScoreName);
	}
}
