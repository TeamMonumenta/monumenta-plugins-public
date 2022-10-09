package com.playmonumenta.plugins.cosmetics.skills.cleric.hierophant;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.Material;

public class HallowedBeamCS implements CosmeticSkill {

	public static final ImmutableMap<String, HallowedBeamCS> SKIN_LIST = ImmutableMap.<String, HallowedBeamCS>builder()
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.HALLOWED_BEAM;
	}

	@Override
	public Material getDisplayItem() {
		return Material.BOW;
	}

	@Override
	public String getName() {
		return null;
	}
}
