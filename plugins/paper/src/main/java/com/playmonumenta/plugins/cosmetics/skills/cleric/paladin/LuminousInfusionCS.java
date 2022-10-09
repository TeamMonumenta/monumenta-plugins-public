package com.playmonumenta.plugins.cosmetics.skills.cleric.paladin;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.Material;

public class LuminousInfusionCS implements CosmeticSkill {

	public static final ImmutableMap<String, LuminousInfusionCS> SKIN_LIST = ImmutableMap.<String, LuminousInfusionCS>builder()
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.LUMINOUS_INFUSION;
	}

	@Override
	public Material getDisplayItem() {
		return Material.BLAZE_POWDER;
	}

	@Override
	public String getName() {
		return null;
	}
}
