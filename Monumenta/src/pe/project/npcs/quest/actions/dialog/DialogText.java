package pe.project.npcs.quest.actions.dialog;

import java.util.ArrayList;
import java.util.Iterator;

import org.bukkit.entity.Player;

import com.google.gson.JsonElement;

import pe.project.Plugin;
import pe.project.utils.MessagingUtils;

public class DialogText implements DialogBase {
	private String mNpcName;
	private ArrayList<String> mText = new ArrayList<String>();

	public DialogText(String npcName, JsonElement element) throws Exception {
		mNpcName = npcName;

		if (element.isJsonPrimitive()) {
			mText.add(element.getAsString());
		} else if (element.isJsonArray()) {
			Iterator<JsonElement> iter = element.getAsJsonArray().iterator();
			while (iter.hasNext()) {
				mText.add(iter.next().getAsString());
			}
		} else {
			throw new Exception("text value is neither an array nor a string!");
		}
	}

	@Override
	public void sendDialog(Plugin plugin, Player player) {
		for (String text : mText) {
			MessagingUtils.sendNPCMessage(player, mNpcName, text);
		}
	}
}
