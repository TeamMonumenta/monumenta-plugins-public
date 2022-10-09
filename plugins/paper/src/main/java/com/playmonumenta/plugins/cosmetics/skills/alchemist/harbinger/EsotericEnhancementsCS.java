package com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.Material;

public class EsotericEnhancementsCS implements CosmeticSkill {

	public static final ImmutableMap<String, EsotericEnhancementsCS> SKIN_LIST = ImmutableMap.<String, EsotericEnhancementsCS>builder()
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.ESOTERIC_ENHANCEMENTS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.CREEPER_HEAD;
	}

	@Override
	public String getName() {
		return null;
	}
}
