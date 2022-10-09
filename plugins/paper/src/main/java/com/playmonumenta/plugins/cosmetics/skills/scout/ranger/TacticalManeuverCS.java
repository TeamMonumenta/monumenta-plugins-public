package com.playmonumenta.plugins.cosmetics.skills.scout.ranger;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.Material;

public class TacticalManeuverCS implements CosmeticSkill {

	public static final ImmutableMap<String, TacticalManeuverCS> SKIN_LIST = ImmutableMap.<String, TacticalManeuverCS>builder()
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.TACTICAL_MANEUVER;
	}

	@Override
	public Material getDisplayItem() {
		return Material.STRING;
	}

	@Override
	public String getName() {
		return null;
	}
}
