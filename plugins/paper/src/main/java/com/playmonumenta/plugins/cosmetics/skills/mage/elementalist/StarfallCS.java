package com.playmonumenta.plugins.cosmetics.skills.mage.elementalist;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.Material;

public class StarfallCS implements CosmeticSkill {

	public static final ImmutableMap<String, StarfallCS> SKIN_LIST = ImmutableMap.<String, StarfallCS>builder()
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.STARFALL;
	}

	@Override
	public Material getDisplayItem() {
		return Material.MAGMA_BLOCK;
	}

	@Override
	public String getName() {
		return null;
	}
}
