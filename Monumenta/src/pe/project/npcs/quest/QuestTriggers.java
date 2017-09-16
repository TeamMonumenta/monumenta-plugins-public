package pe.project.npcs.quest;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class QuestTriggers {
	HashMap<String, QuestTrigger> mTriggers = new HashMap<String, QuestTrigger>();
	
	QuestTriggers(JsonObject object) {
		Iterator<Entry<String, JsonElement>> iter = object.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, JsonElement> entry = iter.next();
			
			mTriggers.put(entry.getKey(), new QuestTrigger(entry.getValue().getAsJsonObject()));
		}
	}
	
	QuestTrigger getTrigger(String triggerName) {
		return mTriggers.get(triggerName);
	}
}
