package pe.project.npcs.quest.prerequisites;

import java.util.ArrayList;
import java.util.Iterator;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.project.utils.InventoryUtils;

public class ItemsInInventoryPrerequisite implements BasePrerequisite {
	class ItemInfo {
		String mItemName = null;	//	Currently unused, future addition.
		Material mItemType = null;	//	Currently unused, future addition.
		String mQuestLoreId = null;
		int mCount = 0;
	}
	
	ArrayList<ItemInfo> mItems = new ArrayList<ItemInfo>();

	public ItemsInInventoryPrerequisite(JsonArray items) {
		Iterator<JsonElement> iter = items.iterator();
		while (iter.hasNext()) {
			JsonElement entry = iter.next();
			
			if (entry.isJsonObject()) {
				ItemInfo info = new ItemInfo();
				
				JsonObject object = entry.getAsJsonObject();
				
				//	QuestLoreID
				JsonElement questLoreId = object.get("quest_lore_id");
				if (questLoreId != null) {
					info.mQuestLoreId = questLoreId.getAsString();
				}
				
				//	Count
				JsonElement count = object.get("count");
				if (count != null) {
					info.mCount = count.getAsInt();
				} else {
					info.mCount = 1;
				}
				
				mItems.add(info);
			}
		}
	}

	@Override
	public boolean prerequisiteMet(Player player) {
		//	Initialize our test variable.
		Integer[] prereqs = new Integer[mItems.size()];
		for (int i = 0; i < mItems.size(); i++) {
			prereqs[i] = mItems.get(i).mCount;
		}
		
		//	Loop through the inventory of the player.
		for (int i = 0; i < player.getInventory().getSize(); i++) {
			ItemStack item = player.getInventory().getItem(i);
			
			//	Loop through all the prerequisites and check against this item.
			for (int j = 0; j < mItems.size(); j++) {
				//	If the prereq isn't met yet go ahead and test.
				if (prereqs[j] > 0) {
					if (_matchesRequiredItem(mItems.get(j), item)) {
						//	If this item is a match for the prereq go ahead and subtract the count.
						prereqs[j] -= item.getAmount();
					}
				}
				
				//	After we checked all the prereqs go ahead and see if we succeeded in all our matches.
				if (_allComplete(prereqs)) {
					return true;
				}
			}
		}

		return false;
	}
	
	private boolean _matchesRequiredItem(ItemInfo info, ItemStack item) {
		//	Check against item name, if there is one set in the info.
		if (info.mItemName != null) {
			if (!item.getItemMeta().getDisplayName().equals(info.mItemName)) {
				return false;
			}
		}
		
		//	Check against item type, if there is one set in the info.
		if (info.mItemType != null) {
			if (!item.getType().equals(info.mItemType)) {
				return false;
			}
		}
		
		//	Check against Lore's, if there is one set in the info.
		if (info.mQuestLoreId != null) {
			if (!InventoryUtils.testForItemWithLore(item, info.mQuestLoreId)) {
				return false;
			}
		}
		
		return true;
	}
	
	private boolean _allComplete(Integer[] items) {
		for (Integer entry : items) {
			if (entry > 0) {
				return false;
			}
		}
		
		return true;
	}
}
