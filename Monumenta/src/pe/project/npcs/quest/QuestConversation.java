package pe.project.npcs.quest;

import org.bukkit.entity.Player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.project.Main;

class QuestConversation {
	private QuestPrerequisites mPrerequisites;
	private String mDialogName;
	
	QuestConversation(Main plugin, JsonObject object) {
		mPrerequisites = new QuestPrerequisites(object.getAsJsonObject("prerequisites"));
		
		JsonElement dialog = object.get("dialog");
		if (dialog != null) {
			mDialogName = dialog.getAsString();
		} else {
			plugin.getLogger().info("A conversation is missing a dialog.");
		}
	}
	
	boolean meetsPrerequisites(Player player) {
		if (mPrerequisites != null) { 
			return mPrerequisites.prerequisitesMet(player);
		}
		
		return false;
	}
	
	String getDialogName() {
		return mDialogName;
	}
}
