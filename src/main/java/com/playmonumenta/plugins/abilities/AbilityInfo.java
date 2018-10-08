package com.playmonumenta.plugins.abilities;

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

	public int classId = 0;

	//If the ability does not require a spec, input a negative number.
	public int specId = -1;
}
