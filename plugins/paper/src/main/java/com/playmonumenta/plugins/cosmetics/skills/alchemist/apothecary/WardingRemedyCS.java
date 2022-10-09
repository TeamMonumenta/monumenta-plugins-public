package com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.Material;

public class WardingRemedyCS implements CosmeticSkill {

	public static final ImmutableMap<String, WardingRemedyCS> SKIN_LIST = ImmutableMap.<String, WardingRemedyCS>builder()
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.WARDING_REMEDY;
	}

	@Override
	public Material getDisplayItem() {
		return Material.GOLDEN_CARROT;
	}

	@Override
	public String getName() {
		return null;
	}
}
