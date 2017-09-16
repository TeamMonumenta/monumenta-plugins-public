package pe.project.npcs.quest;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.project.Main;

class QuestDialogs {
	NpcQuest mParent;
	HashMap<String, QuestDialog> mDialogs = new HashMap<String, QuestDialog>();
	
	QuestDialogs(Main plugin, NpcQuest quest, JsonObject object) {
		mParent = quest;
		
		Iterator<Entry<String, JsonElement>> iter = object.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, JsonElement> entry = iter.next();
			
			mDialogs.put(entry.getKey(), new QuestDialog(plugin, entry.getValue().getAsJsonObject()));
		}
	}
	
	QuestDialog getDialog(String dialogName) {
		return mDialogs.get(dialogName);
	}
}
