package pe.project.npcs.quest.actions.dialog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import org.bukkit.entity.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import pe.project.Plugin;

public class DialogRandomText implements DialogBase {
	private ArrayList<DialogBase> mEntries = new ArrayList<DialogBase>();
	private Random mRandom = new Random();

	public DialogRandomText(String npcName, JsonElement element) throws Exception {
		JsonArray array = element.getAsJsonArray();
		if (array == null) {
			throw new Exception("random_text value is not an array!");
		}

		Iterator<JsonElement> iter = array.iterator();
		while (iter.hasNext()) {
			JsonElement entry = iter.next();

			mEntries.add(new DialogText(npcName, entry));
		}
	}

	@Override
	public void sendDialog(Plugin plugin, Player player) {
		// Select a random entry from the list and send just that
		int idx = mRandom.nextInt(mEntries.size());
		mEntries.get(idx).sendDialog(plugin, player);
	}
}

