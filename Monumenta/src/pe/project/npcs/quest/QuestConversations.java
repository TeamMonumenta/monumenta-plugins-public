package pe.project.npcs.quest;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.bukkit.entity.Player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.project.Plugin;

class QuestConversations {
	NpcQuest mParent;
	HashMap<String, QuestConversation> mConversations = new HashMap<String, QuestConversation>();
	
	QuestConversations(Plugin plugin, NpcQuest quest, JsonObject object) {
		mParent = quest;
		
		Iterator<Entry<String, JsonElement>> iter = object.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, JsonElement> entry = iter.next();
			
			mConversations.put(entry.getKey(), new QuestConversation(plugin, entry.getValue().getAsJsonObject()));
		}
	}
	
	QuestConversation getActiveConversation(Player player) {
		Iterator<Entry<String, QuestConversation>> iter = mConversations.entrySet().iterator();
		
		//	Look through conversations and find the first convo we meet the prerequisites for.
		while (iter.hasNext()) {
			Entry<String, QuestConversation> entry = iter.next();
			
			if (entry.getValue().meetsPrerequisites(player)) {
				return entry.getValue();
			}
		}
		
		return null;
	}
}

