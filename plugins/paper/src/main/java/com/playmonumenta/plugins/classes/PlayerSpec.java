package com.playmonumenta.plugins.classes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.abilities.Ability;
import java.util.ArrayList;
import javax.annotation.Nullable;
import org.bukkit.inventory.ItemStack;


public class PlayerSpec {
	public ArrayList<Ability> mAbilities = new ArrayList<Ability>();

	public @Nullable String mSpecQuestScoreboard;

	public @Nullable String mSpecName;
	public int mSpecialization;
	public @Nullable ItemStack mDisplayItem;
	public @Nullable String mDescription;

	public JsonObject toJson() {
		JsonArray abilities = new JsonArray();
		for (Ability ability : mAbilities) {
			if (ability != null) {
				abilities.add(ability.getInfo().toJson());
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
