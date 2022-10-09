package com.playmonumenta.plugins.cosmetics.skills.warrior.berserker;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.Material;

public class MeteorSlamCS implements CosmeticSkill {

	public static final ImmutableMap<String, MeteorSlamCS> SKIN_LIST = ImmutableMap.<String, MeteorSlamCS>builder()
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.METEOR_SLAM;
	}

	@Override
	public Material getDisplayItem() {
		return Material.FIRE_CHARGE;
	}

	@Override
	public String getName() {
		return null;
	}
}
