package com.playmonumenta.plugins.cosmetics.skills.warlock.tenebrist;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.Material;

public class HauntingShadesCS implements CosmeticSkill {

	public static final ImmutableMap<String, HauntingShadesCS> SKIN_LIST = ImmutableMap.<String, HauntingShadesCS>builder()
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.HAUNTING_SHADES;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SKELETON_SKULL;
	}

	@Override
	public String getName() {
		return null;
	}
}

