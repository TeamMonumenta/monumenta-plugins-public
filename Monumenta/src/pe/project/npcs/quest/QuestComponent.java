package pe.project.npcs.quest;

import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.entity.Player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.project.Plugin;

public class QuestComponent {
	private QuestPrerequisites mPrerequisites = null;
	private QuestActions mActions = null;

	public QuestComponent(String npcName, JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("quest_components value is not an object!");
		}

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			if (!key.equals("prerequisites") && !key.equals("actions")) {
				throw new Exception("Unknown quest_components key: " + key);
			}

			// All quest_components entries are single JSON things that should be passed
			// to their respective handlers
			JsonElement value = object.get(key);
			if (value == null) {
				throw new Exception("quest_components value for key '" + key + "' is not parseable!");
			}

			if (key.equals("prerequisites")) {
				mPrerequisites = new QuestPrerequisites(value);
			} else if (key.equals("actions")) {
				mActions = new QuestActions(npcName, value);
			}
		}

		if (mActions == null) {
			throw new Exception("quest_components value without an action!");
		}
	}

	private boolean prerequisitesMet(Player player) {
		if (mPrerequisites != null) {
			return mPrerequisites.prerequisitesMet(player);
		}

		// Default is no pre-requisites
		return true;
	}

	public void doActionsIfPrereqsMet(Plugin plugin, Player player) {
		if (prerequisitesMet(player)) {
			mActions.doActions(plugin, player);
		}
	}
}
