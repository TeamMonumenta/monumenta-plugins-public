package pe.project.npcs.quest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.entity.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.project.npcs.quest.prerequisites.PrerequisiteBase;
import pe.project.npcs.quest.prerequisites.PrerequisiteCheckScores;
import pe.project.npcs.quest.prerequisites.PrerequisiteItemsInInventory;

class QuestPrerequisites {
	ArrayList<PrerequisiteBase> mPrerequisites = new ArrayList<PrerequisiteBase>();

	QuestPrerequisites(JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("prerequisites value is not an object!");
		}

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();
			JsonElement value = ent.getValue();

			if (key.equals("check_scores")) {
				JsonObject scoreObject = value.getAsJsonObject();
				if (scoreObject == null) {
					throw new Exception("check_scores value is not an object!");
				}

				Set<Entry<String, JsonElement>> scoreEntries = scoreObject.entrySet();
				for (Entry<String, JsonElement> scoreEnt : scoreEntries) {
					mPrerequisites.add(new PrerequisiteCheckScores(scoreEnt.getKey(), scoreEnt.getValue()));
				}
			} else if (key.equals("items_in_inventory")) {
				JsonArray array = value.getAsJsonArray();
				if (array == null) {
					throw new Exception("Prerequisites value for key '" + key + "' is not an array!");
				}

				Iterator<JsonElement> iter = array.iterator();
				while (iter.hasNext()) {
					mPrerequisites.add(new PrerequisiteItemsInInventory(iter.next()));
				}
			} else {
				throw new Exception("Unknown prerequisites key: '" + key + "'");
			}
		}
	}

	boolean prerequisitesMet(Player player) {
		for (PrerequisiteBase prerequisite : mPrerequisites) {
			if (!prerequisite.prerequisiteMet(player)) {
				return false;
			}
		}

		return true;
	}
}
