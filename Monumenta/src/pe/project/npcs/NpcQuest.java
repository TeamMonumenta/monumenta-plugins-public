package pe.project.npcs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.project.Plugin;
import pe.project.npcs.quest.QuestComponent;
import pe.project.utils.FileUtils;

/*
 * A NpcQuest object holds all the quest components belonging to an NPC with a specific name
 * Only one NpcQuest object exists for each NPC with quest interactions
 * If multiple files/quests exist that use the same NPC, their QuestComponent's are
 * combined into a single NpcQuest
 */
public class NpcQuest {
	private ArrayList<QuestComponent> mComponents = new ArrayList<QuestComponent>();
	private String mNpcName;
	private String mDisplayName;
	private EntityType mEntityType;

	public NpcQuest(String fileLocation) throws Exception {
		String content = FileUtils.readFile(fileLocation);
		if (content != null && !content.isEmpty()) {
			Gson gson = new Gson();
			JsonObject object = gson.fromJson(content, JsonObject.class);
			if (object == null) {
				throw new Exception("Failed to parse file '" + fileLocation + "' as JSON object");
			}

			// Read the npc's name first
			JsonElement npc = object.get("npc");
			if (npc == null) {
				throw new Exception("'npc' entry for quest '" + fileLocation + "' is required");
			}
			mNpcName = npc.getAsString();
			if (mNpcName == null) {
				throw new Exception("Failed to parse 'npc' name for file '" +
				                    fileLocation + "' as string");
			}

			// Read the npc's display name
			JsonElement displayName = object.get("display_name");
			if (displayName == null) {
				mDisplayName = mNpcName;
			} else {
				mDisplayName = displayName.getAsString();
				if (mDisplayName == null) {
					mDisplayName = mNpcName;
				}
			}

			// Read the npc's entity_type
			// Default to villager
			JsonElement entityType = object.get("entity_type");
			if (entityType == null) {
				mEntityType = EntityType.VILLAGER;
			} else {
				mEntityType = EntityType.valueOf(entityType.getAsString());
				if (mEntityType == null) {
					mEntityType = EntityType.VILLAGER;
				}
			}

			Set<Entry<String, JsonElement>> entries = object.entrySet();
			for (Entry<String, JsonElement> ent : entries) {
				String key = ent.getKey();

				if (!key.equals("npc") && !key.equals("display_name")
				    && !key.equals("quest_components") && !key.equals("entity_type")) {
					throw new Exception("Unknown quest key: " + key);
				}

				// Note that 'npc' case was handled already - it had to be done first
				if (key.equals("quest_components")) {
					JsonArray array = object.getAsJsonArray(key);
					if (array == null) {
						throw new Exception("Failed to parse 'quest_components' as array for file '" +
						                    fileLocation + "' as JSON object");
					}

					Iterator<JsonElement> iter = array.iterator();
					while (iter.hasNext()) {
						JsonElement entry = iter.next();

						mComponents.add(new QuestComponent(mNpcName, mDisplayName, mEntityType, entry));
					}
				}
			}
		}
	}

	public String getNpcName() {
		return mNpcName;
	}

	public String getDisplayName() {
		return mDisplayName;
	}

	public ArrayList<QuestComponent> getComponents() {
		return mComponents;
	}

	public EntityType getEntityType() {
		return mEntityType;
	}

	// Combines another quest using the same NPC into this one
	public void addFromQuest(Plugin plugin, NpcQuest quest) {
		if (quest.getNpcName().equals(mNpcName)) {
			for (QuestComponent component : quest.getComponents()) {
				mComponents.add(component);
			}
		} else {
			plugin.getLogger().severe("Attempted to add two quests together with different NPCs!");
		}
	}

	// Returns true if any quest components were attempted with this NPC
	// False otherwise
	public boolean interactEvent(Plugin plugin, Player player, String npcName, EntityType entityType) {
		if (mEntityType.equals(entityType) && mNpcName.equals(npcName)) {
			for (QuestComponent component : mComponents) {
				component.doActionsIfPrereqsMet(plugin, player);
			}
			return true;
		}
		return false;
	}
}
