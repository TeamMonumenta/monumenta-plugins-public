package pe.project.npcs.quest;

import java.util.ArrayList;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.project.npcs.quest.actions.*;

class QuestTrigger {
	private ArrayList<BaseAction> mActions = new ArrayList<BaseAction>();
	
	QuestTrigger(JsonObject object) {
		//	Dialog
		JsonElement dialog = object.get("dialog");
		if (dialog != null) {
			mActions.add(new DialogAction(dialog.getAsString()));
		}
		
		//	Set Scores
		JsonElement setScores = object.get("set_scores");
		if (setScores != null) {
			mActions.add(new SetScoresAction(setScores.getAsJsonArray()));
		}
		
		//	Function
		JsonElement function = object.get("function");
		if (function != null) {
			mActions.add(new FunctionAction(function.getAsString()));
		}
	}
	
	ArrayList<BaseAction> getActions() {
		return mActions;
	}
}
