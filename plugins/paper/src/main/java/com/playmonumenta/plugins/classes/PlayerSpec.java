package com.playmonumenta.plugins.classes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import java.util.ArrayList;
import javax.annotation.Nullable;
import org.bukkit.inventory.ItemStack;


public class PlayerSpec {
	public ArrayList<AbilityInfo<?>> mAbilities = new ArrayList<>();

	public @Nullable String mSpecQuestScoreboard;

	public @Nullable String mSpecName;
	public int mSpecialization;
	public @Nullable ItemStack mDisplayItem;
	public @Nullable String mDescription;

	public JsonObject toJson() {
		JsonArray abilities = new JsonArray();
		for (AbilityInfo<?> ability : mAbilities) {
			if (ability != null) {
				abilities.add(ability.toJson());
			}
		}

		JsonObject info = new JsonObject();
		info.addProperty("specId", mSpecialization);
		if (mSpecName != null) {
			info.addProperty("specName", mSpecName);
		}
		if (mSpecQuestScoreboard != null) {
			info.addProperty("specQuestScore", mSpecQuestScoreboard);
		}
		info.add("specSkills", abilities);
		return info;
	}
}
