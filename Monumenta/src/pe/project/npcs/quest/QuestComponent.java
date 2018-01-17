package pe.project.npcs.quest;

import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.project.Plugin;

public class QuestComponent {
	private QuestPrerequisites mPrerequisites = null;
	private QuestActions mActions = null;

	public QuestComponent(String npcName, String displayName,
	                      EntityType entityType, JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("quest_components value is not an object!");
		}

		// Read the delay_actions_by_ticks field first, if specified
		JsonElement delayElement = object.get("delay_actions_by_ticks");
		int delayTicks = 0;
		if (delayElement != null) {
			delayTicks = delayElement.getAsInt();
		}

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			if (!key.equals("prerequisites") && !key.equals("actions")
			&& !key.equals("delay_actions_by_ticks")) {
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
				mActions = new QuestActions(npcName, displayName, entityType, delayTicks, value);
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
