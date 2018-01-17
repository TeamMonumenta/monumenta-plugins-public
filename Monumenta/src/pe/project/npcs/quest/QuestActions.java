package pe.project.npcs.quest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.project.Constants;
import pe.project.Plugin;
import pe.project.npcs.quest.actions.ActionBase;
import pe.project.npcs.quest.actions.ActionCommand;
import pe.project.npcs.quest.actions.ActionDialog;
import pe.project.npcs.quest.actions.ActionFunction;
import pe.project.npcs.quest.actions.ActionRerunComponents;
import pe.project.npcs.quest.actions.ActionSetScores;

public class QuestActions {
	ArrayList<ActionBase> mActions = new ArrayList<ActionBase>();
	int mDelayTicks = 0;

	public QuestActions(String npcName, String displayName, EntityType entityType,
	                    int delayTicks, JsonElement element) throws Exception {
		mDelayTicks = delayTicks;

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

				if (!key.equals("dialog") && !key.equals("set_scores") &&
				    !key.equals("command") && !key.equals("function") &&
				    !key.equals("rerun_components")) {
					throw new Exception("Unknown actions key: " + key);
				}

				// All action entries are single JSON things that should be passed
				// to their respective handlers
				JsonElement value = object.get(key);
				if (value == null) {
					throw new Exception("actions value for key '" + key + "' is not parseable!");
				}

				if (key.equals("dialog")) {
					mActions.add(new ActionDialog(npcName, displayName, entityType, value));
				} else if (key.equals("set_scores")) {
					mActions.add(new ActionSetScores(value));
				} else if (key.equals("command")) {
					mActions.add(new ActionCommand(value));
				} else if (key.equals("function")) {
					mActions.add(new ActionFunction(value));
				} else if (key.equals("rerun_components")) {
					mActions.add(new ActionRerunComponents(npcName, entityType));
				}
			}
		}
	}

	public void doActions(Plugin plugin, Player player) {
		if (mDelayTicks <= 0) {
			// If not delayed, actions can run without restrictions
			for (ActionBase action : mActions) {
				action.doAction(plugin, player);
			}
		} else {
			// If delayed, only one delayed group of actions is allowed per player
			if (!player.hasMetadata(Constants.PLAYER_QUEST_ACTIONS_LOCKED_METAKEY)) {
				player.setMetadata(Constants.PLAYER_QUEST_ACTIONS_LOCKED_METAKEY,
				                   new FixedMetadataValue(plugin, true));

				player.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					@Override
					public void run() {
						for (ActionBase action : mActions) {
							action.doAction(plugin, player);
						}

						player.removeMetadata(Constants.PLAYER_QUEST_ACTIONS_LOCKED_METAKEY, plugin);
					}
				}, mDelayTicks);
			}
		}
	}
}
