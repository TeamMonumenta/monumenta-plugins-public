package pe.project.npcs.quest.prerequisites;

import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.entity.Player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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

	public PrerequisiteCheckScores(JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("check_scores value is not an object!");
		}

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			mScoreName = ent.getKey();
			JsonElement value = ent.getValue();

			if (value.isJsonPrimitive()) {
				//	Single value
				Integer score = value.getAsInt();
				if (score == null) {
					throw new Exception("check_score value for scoreboard '" + mScoreName + "' is not an integer");
				}

				mRange = new ScoreRange(score);
			} else {
				//	Has a min/max
				JsonObject scoreObject = value.getAsJsonObject();
				if (scoreObject == null) {
					throw new Exception("check_score value for scoreboard '" + mScoreName + "' is an unparseable object");
				}

				JsonElement min = scoreObject.get("min");
				JsonElement max = scoreObject.get("max");
				if (min == null || max == null) {
					throw new Exception("check_score value for scoreboard '" + mScoreName + "' is an object but without min and max");
				}

				Integer imin = min.getAsInt();
				Integer imax = max.getAsInt();
				if (imin == null || imax == null) {
					throw new Exception("check_score value for scoreboard '" + mScoreName + "' is an object but min/max are not integers");
				}

				mRange = new ScoreRange(imin, imax);
			}
		}
	}

	@Override
	public boolean prerequisiteMet(Player player) {
		return mRange.withinRange(ScoreboardUtils.getScoreboardValue(player, mScoreName));
	}
}
