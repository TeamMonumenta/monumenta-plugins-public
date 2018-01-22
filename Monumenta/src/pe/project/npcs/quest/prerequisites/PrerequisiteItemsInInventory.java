package pe.project.npcs.quest.prerequisites;

import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.project.utils.InventoryUtils;

public class PrerequisiteItemsInInventory implements PrerequisiteBase {
	String mName = "";
	String mLore = "";
	Material mType = Material.AIR;
	int mCount = 1;

	public PrerequisiteItemsInInventory(JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("items_in_inventory value is not an object!");
		}

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();
			JsonElement value = ent.getValue();

			if (!key.equals("lore") && !key.equals("name") && !key.equals("count") && !key.equals("type")) {
				throw new Exception("Unknown items_in_inventory key: " + key);
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
			} else if (key.equals("type")) {
				String typeStr = value.getAsString();
				if (typeStr == null) {
					throw new Exception("items_in_inventory type entry is not a string!");
				}
				try {
					mType = Material.valueOf(typeStr);
				} catch (IllegalArgumentException e) {
					throw new Exception("Unknown Material '" + typeStr +
					                    "' - it should be one of the values in this list: " +
					                    "https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html");
				}
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
			    InventoryUtils.testForItemWithLore(item, mLore) &&
			    (mType.equals(Material.AIR) || mType.equals(item.getType()))) {
				matchCount += item.getAmount();
			}

			if (mCount <= 0 && matchCount > 0) {
				// Found an item where none should be - fail
				return false;
			} else if (mCount > 0 && matchCount >= mCount) {
				// Found at least the correct number of items
				return true;
			}
		}

		// Searched entire inventory and didn't find any when didn't expect to
		if (mCount <= 0 && matchCount <= 0) {
			return true;
		}

		return false;
	}
}
