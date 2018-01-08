package pe.project.npcs.quest.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.entity.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.project.Plugin;
import pe.project.npcs.quest.actions.dialog.DialogBase;
import pe.project.npcs.quest.actions.dialog.DialogClickableText;
import pe.project.npcs.quest.actions.dialog.DialogRawText;
import pe.project.npcs.quest.actions.dialog.DialogText;

public class ActionDialog implements ActionBase {
	ArrayList<DialogBase> mDialogs = new ArrayList<DialogBase>();

	public ActionDialog(String npcName, JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("dialog value is not an object!");
		}

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			if (!key.equals("text") && !key.equals("raw_text") && !key.equals("clickable_text")) {
				throw new Exception("Unknown dialog key: '" + key + "'");
			}

			// clickable_text is a special case
			//  the whole array of options needs to be one DialogClickableText object
			if (key.equals("clickable_text")) {
				mDialogs.add(new DialogClickableText(npcName, ent.getValue()));
				continue;
			}

			// The remaining options - text and raw_text - are JSON arrays
			JsonArray array = object.getAsJsonArray(key);
			if (array == null) {
				throw new Exception("Dialog value for key '" + key + "' is not an array!");
			}

			Iterator<JsonElement> iter = array.iterator();
			while (iter.hasNext()) {
				JsonElement entry = iter.next();

				if (key.equals("text")) {
					mDialogs.add(new DialogText(npcName, entry));
				} else if (key.equals("raw_text")) {
					mDialogs.add(new DialogRawText(entry));
				}
			}
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player) {
		for (DialogBase dialog : mDialogs) {
			dialog.sendDialog(plugin, player);
		}
	}
}
