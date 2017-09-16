package pe.project.npcs.quest;

import org.bukkit.entity.Player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.project.Main;
import pe.project.utils.MessagingUtils;

class QuestOption {
	static private final String COMMAND_STRING = "/QuestTrigger %s %s %s %s";
	
	String mText;
	String mTrigger;
	
	QuestOption(Main plugin, JsonObject object) {
		JsonElement text = object.get("text");
		if (text != null) {
			mText = text.getAsString();
		} else {
			plugin.getLogger().info("A Quest Option is missing text.");
		}
		
		JsonElement trigger = object.get("trigger");
		if (trigger != null) {
			mTrigger = trigger.getAsString();
		} else {
			plugin.getLogger().info("A Quest Option is missing a Trigger.");
		}
	}
	
	void display(Main plugin, Player player, String npcName, String questName) {
		String squashedName = npcName.replaceAll("\\s+", "");
		String triggerStr = String.format(COMMAND_STRING, player.getName(), squashedName, questName, mTrigger);	
		MessagingUtils.sendClickableNPCMessage(plugin, player, mText, triggerStr);
	}
}
