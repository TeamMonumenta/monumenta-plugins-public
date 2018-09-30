package com.playmonumenta.plugins.abilities;

import com.playmonumenta.plugins.Plugin.Classes;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.specializations.ClassSpecialization;

/**
 * The AbilityInfo class contains the small information bits
 * about an ability. This is to keep the information compact and
 * not have a bunch of getters and setters of data that is menial.
 * @author FirelordWeaponry (Fire)
 *
 */
public class AbilityInfo {

	private final Ability ability;

	public String name = null;
	public String scoreboardId = null;
	public Spells linkedSpell = null;
	public AbilityTrigger trigger = null;
	public int cooldown = 0;
	public int classId = 0;
	public int specId = 0;

	public AbilityInfo(Ability ability) {
		this.ability = ability;
	}

	public Ability getAbility() { return ability; }

}
