package pe.project.npcs.quest.actions.dialog;

import org.bukkit.entity.Player;

import com.google.gson.JsonElement;

import pe.project.Plugin;
import pe.project.utils.MessagingUtils;

public class DialogText implements DialogBase {
	private String mNpcName;
	private String mText;

	public DialogText(String npcName, JsonElement element) throws Exception {
		mNpcName = npcName;
		mText = element.getAsString();
		if (mText == null) {
			throw new Exception("Dialog text entry is not a string!");
		}
	}

	@Override
	public void sendDialog(Plugin plugin, Player player) {
		MessagingUtils.sendNPCMessage(player, mNpcName, mText);
	}
}
