package pe.project.npcs.quest.prerequisites;

import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.project.utils.InventoryUtils;

public class PrerequisiteItemsInInventory implements PrerequisiteBase {
	String mName = "";
	String mLore = "";
	int mCount = 1;

	public PrerequisiteItemsInInventory(JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("items_in_inventory value is not an object!");
		}

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			if (!key.equals("lore") && !key.equals("name") && !key.equals("count")) {
				throw new Exception("Unknown items_in_inventory key: " + key);
			}

			// All quest_components entries are single JSON things that should be passed
			// to their respective handlers
			JsonElement value = object.get(key);
			if (value == null) {
				throw new Exception("items_in_inventory value for key '" + key + "' is not parseable!");
			}

			if (key.equals("lore")) {
				mLore = value.getAsString();
				if (mLore == null) {
					throw new Exception("items_in_inventory lore entry is not a string!");
				}
			} else if (key.equals("name")) {
				mName = value.getAsString();
				if (mName == null) {
					throw new Exception("items_in_inventory name entry is not a string!");
				}
			} else if (key.equals("count")) {
				mCount = value.getAsInt();
			}
		}
	}

	@Override
	public boolean prerequisiteMet(Player player) {
		int matchCount = 0;

		//  Loop through the inventory of the player.
		for (int i = 0; i < player.getInventory().getSize(); i++) {
			ItemStack item = player.getInventory().getItem(i);

			if (InventoryUtils.testForItemWithName(item, mName) &&
			    InventoryUtils.testForItemWithLore(item, mLore)) {
				matchCount += item.getAmount();
			}

			if (mCount <= 0 && matchCount > 0) {
				// Found an item where none should be - fail
				return false;
			} else if (matchCount >= mCount) {
				// Found at least the correct number of items
				return true;
			}
		}

		return false;
	}
}
