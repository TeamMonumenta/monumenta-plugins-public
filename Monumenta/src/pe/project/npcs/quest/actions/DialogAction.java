package pe.project.npcs.quest.actions;

import org.bukkit.entity.Player;

import pe.project.Plugin;

public class DialogAction implements BaseAction {
	private String mDialogName;
	
	public DialogAction(String dialogName) {
		mDialogName = dialogName;
	}

	@Override
	public actionType getType() {
		return actionType.Dialog;
	}

	@Override
	public void trigger(Plugin plugin, Player player) {
		//	Do nothing.
	}
	
	public String getDialogName() {
		return mDialogName;
	}

}
