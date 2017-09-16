package pe.project.npcs.quest;

import java.util.ArrayList;

import org.bukkit.entity.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import pe.project.npcs.quest.prerequisites.BasePrerequisite;
import pe.project.npcs.quest.prerequisites.CheckScoresPrerequisite;
import pe.project.npcs.quest.prerequisites.ItemsInInventoryPrerequisite;

class QuestPrerequisites {
	ArrayList<BasePrerequisite> mPrerequisites = new ArrayList<BasePrerequisite>();
	
	QuestPrerequisites(JsonObject object) {
		//	CheckScores
		JsonArray scores = object.getAsJsonArray("check_scores");
		if (scores != null) {
			mPrerequisites.add(new CheckScoresPrerequisite(scores));
		}
		
		//	ItemsInInventory
		JsonArray items = object.getAsJsonArray("items_in_inventory");
		if (items != null) {
			mPrerequisites.add(new ItemsInInventoryPrerequisite(items));
		}
	}
	
	boolean prerequisitesMet(Player player) {
		for (BasePrerequisite prerequisite : mPrerequisites) {
			if (!prerequisite.prerequisiteMet(player)) {
				return false;
			}
		}
		
		return true;
	}
}
