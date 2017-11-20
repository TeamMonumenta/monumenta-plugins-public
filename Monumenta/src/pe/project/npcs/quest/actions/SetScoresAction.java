package pe.project.npcs.quest.actions;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.bukkit.entity.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import pe.project.Plugin;
import pe.project.utils.ScoreboardUtils;

public class SetScoresAction implements BaseAction {
	HashMap<String, Integer> mScoresToSet = new HashMap<String, Integer>();
	
	public SetScoresAction(JsonArray objects) {
		Iterator<JsonElement> iter = objects.iterator();
		while (iter.hasNext()) {
			JsonElement entry = iter.next();
			
			Iterator<Entry<String, JsonElement>> scoreIter = entry.getAsJsonObject().entrySet().iterator();
			while (scoreIter.hasNext()) {
				Entry<String, JsonElement> scoreEntry = scoreIter.next();
				
				mScoresToSet.put(scoreEntry.getKey(), scoreEntry.getValue().getAsInt());
			}
		}
	}

	@Override
	public actionType getType() {
		return actionType.SetScores;
	}

	@Override
	public void trigger(Plugin plugin, Player player) {
		//	Loop through all our scores to set, and set them on the passed in player.
		Iterator<Entry<String, Integer>> iter = mScoresToSet.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, Integer> entry = iter.next();
			
			ScoreboardUtils.setScoreboardValue(player, entry.getKey(), entry.getValue());
		}
	}
}
