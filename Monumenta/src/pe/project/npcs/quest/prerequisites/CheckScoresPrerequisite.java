package pe.project.npcs.quest.prerequisites;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.bukkit.entity.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.project.utils.ScoreboardUtils;

public class CheckScoresPrerequisite implements BasePrerequisite {
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
	
	HashMap<String, ScoreRange> mScores = new HashMap<String, ScoreRange>();
	
	public CheckScoresPrerequisite(JsonArray scores) {
		Iterator<JsonElement> iter = scores.iterator();
		while (iter.hasNext()) {
			JsonElement entry = iter.next();
			Iterator<Entry<String, JsonElement>> scoreIter = entry.getAsJsonObject().entrySet().iterator();
			while (scoreIter.hasNext()) {
				Entry<String, JsonElement> scoreEntry = scoreIter.next();
				
				JsonElement value = scoreEntry.getValue();
				
				//	Single value
				if (value.isJsonPrimitive()) {
					mScores.put(scoreEntry.getKey(), new ScoreRange(value.getAsInt()));
				}
				//	Has a min/max
				else {
					JsonObject object = value.getAsJsonObject();
					
					JsonElement min = object.get("min");
					JsonElement max = object.get("max");
					if (min != null && max != null) {
						mScores.put(scoreEntry.getKey(), new ScoreRange(min.getAsInt(), max.getAsInt()));
					}
				}
			}
		}
	}
	
	@Override
	public boolean prerequisiteMet(Player player) {
		Iterator<Entry<String, ScoreRange>> iter = mScores.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, ScoreRange> scoreEntry = iter.next();
			
			ScoreRange range = scoreEntry.getValue();
			if (!range.withinRange(ScoreboardUtils.getScoreboardValue(player, scoreEntry.getKey()))) {
				return false;
			}
		}
		
		return true;
	}
}
