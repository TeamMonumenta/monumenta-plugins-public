package pe.project.npcs.quest;

import org.bukkit.entity.Player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.project.Plugin;

class QuestInfo {
	String mQuestName;
	QuestPrerequisites mPrerequisites;
	
	QuestInfo(Plugin plugin, JsonObject object) {
		JsonElement name = object.get("name");
		if (name != null) {
			mQuestName = name.getAsString();
		} else {
			plugin.getLogger().info("A quest info in missing a name.");
		}
		
		mPrerequisites = new QuestPrerequisites(object.getAsJsonObject("prerequisites"));
	}
	
	boolean prerequisitesMet(Player player) {
		if (mPrerequisites != null) {
			return mPrerequisites.prerequisitesMet(player);
		}
		
		return false;
	}
}
