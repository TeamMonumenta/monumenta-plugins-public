package com.playmonumenta.plugins.classes;

import java.util.ArrayList;

import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.abilities.Ability;


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
