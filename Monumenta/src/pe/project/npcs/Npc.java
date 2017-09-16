package pe.project.npcs;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;

import org.bukkit.entity.Player;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.project.Main;
import pe.project.npcs.quest.NpcQuest;
import pe.project.utils.FileUtils;

public class Npc {
	static private final String QUESTS_FILE_LOCATION = File.separator + "npcs" + File.separator + "quests" + File.separator;
	String mName;
	int mIdleDialogSize = 0;
	ArrayList<String> mIdleDialogs = new ArrayList<String>();
	HashMap<String, NpcQuest> mQuest = new HashMap<String, NpcQuest>();
	
	public Npc(Main plugin, String fileLocation) {
		try {
			String content = FileUtils.readFile(fileLocation);
			if (content != null && content != "") {
				Gson gson = new Gson();
				JsonObject object = gson.fromJson(content, JsonObject.class);
				if (object != null) {
					//	Load NPC Name.
					JsonElement name = object.get("name");
					if (name != null) {
						mName = name.getAsString();
					}
					
					//	Load quest and populate out Npc's hashmap.
					JsonArray quests = object.getAsJsonArray("quests");
					if (quests != null) {
						final String questFileLocation = plugin.getDataFolder() + QUESTS_FILE_LOCATION;
						
						Iterator<JsonElement> iter = quests.iterator();
						while (iter.hasNext()) {
							JsonElement entry = iter.next();
							String questName = entry.getAsString();
							
							mQuest.put(questName, new NpcQuest(plugin, questFileLocation + questName, questName, this));
						}
					}
					
					//	Load idle dialogs.
					JsonArray idleDialogs = object.getAsJsonArray("idle_dialogs");
					if (idleDialogs != null) {
						Iterator<JsonElement> iter = idleDialogs.iterator();
						while (iter.hasNext()) {
							JsonElement entry = iter.next();
							
							mIdleDialogs.add(entry.getAsString());
						}
						
						mIdleDialogSize = mIdleDialogs.size();
					}
				}
			}
		} catch (Exception e) {
			plugin.getLogger().severe("Caught exception: " + e);
			e.printStackTrace();
		}
	}
	
	public void interactEvent(Player player, Random rand) {
		Iterator<Entry<String, NpcQuest>> iter = mQuest.entrySet().iterator();
		while (iter.hasNext()) {
			NpcQuest quest = iter.next().getValue();
			if (quest.prerequisitesMet(player)) {
				quest.interactEvent(player, mName);
				return;
			}
		}
		
		//	Since we met none of the quest prerequisites we want to play an idle message instead.
		if (mIdleDialogSize > 0) {
			player.sendMessage(mIdleDialogs.get(rand.nextInt(mIdleDialogSize)));
		}
	}
	
	public void triggerEvent(Player player, String questID, String eventName) {
		NpcQuest quest = mQuest.get(questID);
		if (quest != null) {
			quest.triggerEvent(player, mName, eventName);
		}
	}
	
	public String getName() {
		return mName;
	}
}
