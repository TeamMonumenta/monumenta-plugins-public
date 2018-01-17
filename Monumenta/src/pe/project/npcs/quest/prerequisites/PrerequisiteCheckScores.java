package pe.project.npcs.quest.prerequisites;

import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.entity.Player;

import com.google.gson.JsonElement;

import pe.project.utils.ScoreboardUtils;

public class PrerequisiteCheckScores implements PrerequisiteBase {
	protected class ScoreRange {
		int mMin;
		int mMax;

		ScoreRange(int value) {
			mMin = mMax = value;
		}

		ScoreRange(int min, int max) {
			mMin = min;
			mMax = max;
		}

		boolean withinRange(int value) {
			return value >= mMin && value <= mMax;
		}
	}

	String mScoreName;
	ScoreRange mRange;

	public PrerequisiteCheckScores(String scoreName, JsonElement value) throws Exception {
		mScoreName = scoreName;

		if (value.isJsonPrimitive()) {
			//	Single value
			Integer score = value.getAsInt();
			if (score == null) {
				throw new Exception("check_score value for scoreboard '" + mScoreName + "' is not an integer");
			}

			mRange = new ScoreRange(score);
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

			mRange = new ScoreRange(imin, imax);
		}
	}

	@Override
	public boolean prerequisiteMet(Player player) {
		return mRange.withinRange(ScoreboardUtils.getScoreboardValue(player, mScoreName));
	}
}
