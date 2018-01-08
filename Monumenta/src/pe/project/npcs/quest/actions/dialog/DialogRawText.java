package pe.project.npcs.quest.actions.dialog;

import org.bukkit.entity.Player;

import com.google.gson.JsonElement;

import pe.project.Plugin;

public class DialogRawText implements DialogBase {
	private String mText;

	public DialogRawText(JsonElement element) throws Exception {
		mText = element.getAsString();
		if (mText == null) {
			throw new Exception("Dialog text entry is not a string!");
		}
	}

	@Override
	public void sendDialog(Plugin plugin, Player player) {
		//TODO
		//MessagingUtils.sendRawMessage(plugin, player, mText);
	}
}
