package pe.project.npcs.quest.actions.dialog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import org.bukkit.entity.Player;

import com.google.gson.JsonElement;

import pe.project.Plugin;
import pe.project.utils.MessagingUtils;

public class DialogRandomText implements DialogBase {
	private String mDisplayName;
	private ArrayList<String> mText = new ArrayList<String>();
	private Random mRandom = new Random();

	public DialogRandomText(String displayName, JsonElement element) throws Exception {
		mDisplayName = displayName;

		if (element.isJsonPrimitive()) {
			mText.add(element.getAsString());
		} else if (element.isJsonArray()) {
			Iterator<JsonElement> iter = element.getAsJsonArray().iterator();
			while (iter.hasNext()) {
				mText.add(iter.next().getAsString());
			}
		} else {
			throw new Exception("random_text value is neither an array nor a string!");
		}
	}

	@Override
	public void sendDialog(Plugin plugin, Player player) {
		int idx = mRandom.nextInt(mText.size());
		MessagingUtils.sendNPCMessage(player, mDisplayName, mText.get(idx));
	}
}

