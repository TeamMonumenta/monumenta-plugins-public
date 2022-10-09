package com.playmonumenta.plugins.cosmetics.skills.warrior.guardian;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.Material;

public class ShieldWallCS implements CosmeticSkill {

	public static final ImmutableMap<String, ShieldWallCS> SKIN_LIST = ImmutableMap.<String, ShieldWallCS>builder()
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.SHIELD_WALL;
	}

	@Override
	public Material getDisplayItem() {
		return Material.COBBLESTONE_WALL;
	}

	@Override
	public String getName() {
		return null;
	}
}
