package pe.project.npcs.quest;

import java.util.ArrayList;
import java.util.Iterator;

import org.bukkit.entity.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.project.Plugin;
import pe.project.utils.MessagingUtils;

class QuestDialog {
	String mText;
	String mTrigger;
	ArrayList<QuestOption> mOptions = new ArrayList<QuestOption>();
	
	QuestDialog(Plugin plugin, JsonObject object) {
		JsonElement text = object.get("text");
		if (text != null) {
			mText = text.getAsString();
		} else {
			plugin.getLogger().info("A Quest Dialog is missing text.");
		}
		
		JsonArray options = object.getAsJsonArray("options");
		if (options != null) {
			Iterator<JsonElement> iter = options.iterator();
			while (iter.hasNext()) {
				JsonElement entry = iter.next();
				
				mOptions.add(new QuestOption(plugin, entry.getAsJsonObject()));
			}
		}
		
		JsonElement trigger = object.get("trigger");
		if (trigger != null) {
			mTrigger = trigger.getAsString();
		}
	}
	
	void display(Plugin plugin, Player player, String npcName, String questName) {
		//	Display the Dialog Text.
		MessagingUtils.sendNPCMessage(plugin, player, npcName, mText);
		
		//	Than loop through and display the option text.
		for (QuestOption option : mOptions) {
			option.display(plugin, player, npcName, questName);
		}
	}
	
	String getTrigger() {
		return mTrigger;
	}
}
