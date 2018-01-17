package pe.project.npcs.quest.actions.dialog;

import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.entity.Player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.project.Plugin;
import pe.project.npcs.quest.QuestActions;
import pe.project.utils.MessagingUtils;

public class DialogClickableTextEntry implements DialogBase {
	private String mText;
	private QuestActions mActions;
	private int mIdx;

	public DialogClickableTextEntry(String npcName, String displayName, JsonElement element, int elementIdx) throws Exception {
		mIdx = elementIdx;

		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("dialog value is not an object!");
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

			if (!key.equals("player_text") && !key.equals("actions") && !key.equals("delay_actions_by_ticks")) {
				throw new Exception("Unknown clickable_text key: " + key);
			}

			// All quest_components entries are single JSON things that should be passed
			// to their respective handlers
			JsonElement value = object.get(key);
			if (value == null) {
				throw new Exception("clickable_text value for key '" + key + "' is not parseable!");
			}

			if (key.equals("player_text")) {
				mText = value.getAsString();
				if (mText == null) {
					throw new Exception("clickable_text player_text entry is not a string!");
				}
			} else if (key.equals("actions")) {
				mActions = new QuestActions(npcName, displayName, delayTicks, value);
			}
		}

		if (mActions == null) {
			throw new Exception("clickable_text value without an action!");
		}
	}

	@Override
	public void sendDialog(Plugin plugin, Player player) {
		MessagingUtils.sendClickableNPCMessage(plugin, player, mText, "/questtrigger " + Integer.toString(mIdx));
	}

	public void doActionsIfIdxMatches(Plugin plugin, Player player, int idx) {
		if (idx == mIdx) {
			mActions.doActions(plugin, player);
		}
	}
}

