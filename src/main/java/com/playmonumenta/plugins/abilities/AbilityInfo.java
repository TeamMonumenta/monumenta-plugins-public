package com.playmonumenta.plugins.abilities;

import com.google.gson.JsonObject;

import com.playmonumenta.plugins.classes.Spells;

/**
 * The AbilityInfo class contains the small information bits
 * about an ability. This is to keep the information compact and
 * not have a bunch of getters and setters of data that is menial.
 * @author FirelordWeaponry (Fire)
 *
 */
public class AbilityInfo {
	//If the ability does not require a scoreboardID andj ust a classId, leave this as null.
	public String scoreboardId = null;

	public Spells linkedSpell = null;
	public AbilityTrigger trigger = null;

	//This is in ticks
	public int cooldown = 0;

	/*
	 * If this is set to true, methods of the class will be called even when the skill
	 * is still on cooldown. This is needed for some skills that have multiple possible
	 * triggers or need to catch events after the skill has been put on cooldown
	 */
	public boolean ignoreCooldown = false;

	public int classId = 0;

	//If the ability does not require a spec, input a negative number.
	public int specId = -1;

	public JsonObject getAsJsonObject() {
		JsonObject info = new JsonObject();
		if (scoreboardId != null) {
			info.addProperty("scoreboardId", scoreboardId);
		}
		if (linkedSpell != null) {
			info.addProperty("name", linkedSpell.getName());
		}
		if (trigger != null) {
			info.addProperty("trigger", trigger.toString());
		}
		info.addProperty("cooldown", cooldown);
		info.addProperty("classId", classId);
		info.addProperty("specId", specId);

		return info;
	}
}
