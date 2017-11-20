package pe.project.npcs.quest;

import java.util.ArrayList;

import org.bukkit.entity.Player;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import pe.project.Plugin;
import pe.project.npcs.Npc;
import pe.project.npcs.quest.actions.BaseAction;
import pe.project.npcs.quest.actions.BaseAction.actionType;
import pe.project.utils.FileUtils;
import pe.project.npcs.quest.actions.DialogAction;

public class NpcQuest {
	Plugin mPlugin;
	Npc mParentNpc;
	String mRawQuestName;
	QuestInfo mQuestInfo;
	QuestConversations mConversations;
	QuestDialogs mDialogs;
	QuestTriggers mTriggers;
	
	public NpcQuest(Plugin plugin, String fileLocation, String rawQuestName, Npc npc) {
		mPlugin = plugin;
		mRawQuestName = rawQuestName;
		mParentNpc = npc;
		
		try {
			String content = FileUtils.readFile(fileLocation + ".json");
			if (content != null && !content.isEmpty()) {	
				Gson gson = new Gson();
				JsonObject object = gson.fromJson(content, JsonObject.class);
				
				mQuestInfo = new QuestInfo(mPlugin, object.getAsJsonObject("quest_info"));
				mConversations = new QuestConversations(mPlugin, this, object.getAsJsonObject("conversations"));
				mDialogs = new QuestDialogs(mPlugin, this, object.getAsJsonObject("dialogs"));
				mTriggers = new QuestTriggers(object.getAsJsonObject("triggers"));
			}
		} catch (Exception e) {
			mPlugin.getLogger().severe("Caught exception: " + e);
			e.printStackTrace();
		}
	}
	
	public boolean prerequisitesMet(Player player) {
		if (mQuestInfo != null) {
			return mQuestInfo.prerequisitesMet(player);
		}
		
		return false;
	}
	
	public boolean interactEvent(Player player, String npcName) {
		//	Find the current convo.
		QuestConversation currentConvo = mConversations.getActiveConversation(player);
		if (currentConvo != null) {
			//	Once we know we have a convo, we want to go ahead and grab the dialog for it.
			String dialogName = currentConvo.getDialogName();
			return _handleDialog(player, npcName, dialogName);
		}
		
		return false;
	}
	
	public void triggerEvent(Player player, String npcName, String eventName) {
		QuestTrigger trigger = mTriggers.getTrigger(eventName);
		if (trigger != null) {
			ArrayList<BaseAction> actions = trigger.getActions();
			if (actions != null) {
				for (BaseAction action : actions) {
					//	Trigger the action.
					action.trigger(mPlugin, player);
					
					//	If this action is a dialog we want to also trigger a quest dialog.
					if (action.getType() == actionType.Dialog) {
						String dialogName = ((DialogAction)action).getDialogName();
						_handleDialog(player, npcName, dialogName);
					}
				}
			}
		}
	}
	
	private boolean _handleDialog(Player player, String npcName, String dialogName) {
		if (dialogName != null && !dialogName.isEmpty()) {
			//	Now that we know the dialog we want, go ahead and grab that dialog.
			QuestDialog dialog = mDialogs.getDialog(dialogName);
			if (dialog != null) {
				//	Display some dialog.
				dialog.display(mPlugin, player, npcName, mRawQuestName);
				
				String trigger = dialog.getTrigger();
				if (trigger != null && !trigger.isEmpty()) {
					triggerEvent(player, mParentNpc.getName(), trigger);
				}
				
				return true;
			} else {
				//	Something fucked up and is missing. In this case the specified dialog.
				String message = String.format("Missing Dialog - %s from %s for the Quest %s", dialogName, npcName, mRawQuestName);
				mPlugin.getLogger().info(message);
			}
		} else {
			//	Something fucked up and is missing. In this case a dialog name.
			String message = String.format("Missing or Empty Dialog Name for %s", npcName);
			mPlugin.getLogger().info(message);
		}
		
		return false;
	}
}
