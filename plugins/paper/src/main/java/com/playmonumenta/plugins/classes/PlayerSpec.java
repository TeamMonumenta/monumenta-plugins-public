package com.playmonumenta.plugins.classes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import java.util.ArrayList;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

// fields are initialised in subclasses
@SuppressWarnings("NullAway.Init")
public class PlayerSpec {
	public ArrayList<AbilityInfo<?>> mAbilities = new ArrayList<>();

	public String mSpecQuestScoreboard;

	public String mSpecName;
	public int mSpecialization;
	public Material mDisplayItem;
	public String mDescription;
	public @Nullable AbilityInfo<?> mPassive;

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
		if (mPassive != null) {
			info.add("specPassive", mPassive.toJson());
		}
		if (mSpecQuestScoreboard != null) {
			info.addProperty("specQuestScore", mSpecQuestScoreboard);
		}
		info.add("specSkills", abilities);
		return info;
	}
}
