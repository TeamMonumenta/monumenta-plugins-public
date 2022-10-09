package com.playmonumenta.plugins.cosmetics.skills.rogue.assassin;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.Material;

public class BodkinBlitzCS implements CosmeticSkill {

	public static final ImmutableMap<String, BodkinBlitzCS> SKIN_LIST = ImmutableMap.<String, BodkinBlitzCS>builder()
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.BODKIN_BLITZ;
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
