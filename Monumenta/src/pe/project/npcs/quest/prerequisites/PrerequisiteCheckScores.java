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
				Integer imin = Integer.MIN_VALUE;
				Integer imax = Integer.MAX_VALUE;

				Set<Entry<String, JsonElement>> subentries = value.getAsJsonObject().entrySet();
				for (Entry<String, JsonElement> subent : subentries) {
					String key = subent.getKey();

					if (key.equals("min")) {
						imin = subent.getValue().getAsInt();
					} else if (key.equals("max")) {
						imax = subent.getValue().getAsInt();
					} else {
						throw new Exception("Unknown check_score value: '" + key + "'");
					}
				}

				if (imin == Integer.MIN_VALUE && imax == Integer.MAX_VALUE) {
					throw new Exception("Bogus check_score object with no min or max");
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
