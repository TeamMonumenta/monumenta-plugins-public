package pe.project.npcs.quest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.entity.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.project.Plugin;
import pe.project.npcs.quest.actions.ActionBase;
import pe.project.npcs.quest.actions.ActionDialog;
import pe.project.npcs.quest.actions.ActionFunction;
import pe.project.npcs.quest.actions.ActionSetScores;

public class QuestActions {
	ArrayList<ActionBase> mActions = new ArrayList<ActionBase>();

	public QuestActions(String npcName, JsonElement element) throws Exception {
		JsonArray array = element.getAsJsonArray();
		if (array == null) {
			throw new Exception("actions value is not an array!");
		}

		// Add all array entries
		Iterator<JsonElement> iter = array.iterator();
		while (iter.hasNext()) {
			JsonObject object = iter.next().getAsJsonObject();
			if (object == null) {
				throw new Exception("actions value is not an object!");
			}

			// Add all actions in each entry object
			Set<Entry<String, JsonElement>> entries = object.entrySet();
			for (Entry<String, JsonElement> ent : entries) {
				String key = ent.getKey();

				if (!key.equals("dialog") && !key.equals("set_scores") && !key.equals("function")) {
					throw new Exception("Unknown actions key: " + key);
				}

				// All action entries are single JSON things that should be passed
				// to their respective handlers
				JsonElement value = object.get(key);
				if (value == null) {
					throw new Exception("actions value for key '" + key + "' is not parseable!");
				}

				if (key.equals("dialog")) {
					mActions.add(new ActionDialog(npcName, value));
				} else if (key.equals("set_scores")) {
					mActions.add(new ActionSetScores(value));
				} else if (key.equals("function")) {
					mActions.add(new ActionFunction(value));
				}
			}
		}
	}

	public void doActions(Plugin plugin, Player player) {
		for (ActionBase action : mActions) {
			action.doAction(plugin, player);
		}
	}
}
