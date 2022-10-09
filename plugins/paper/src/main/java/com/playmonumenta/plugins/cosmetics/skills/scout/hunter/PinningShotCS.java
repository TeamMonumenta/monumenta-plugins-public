package com.playmonumenta.plugins.cosmetics.skills.scout.hunter;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.Material;

public class PinningShotCS implements CosmeticSkill {

	public static final ImmutableMap<String, PinningShotCS> SKIN_LIST = ImmutableMap.<String, PinningShotCS>builder()
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.PINNING_SHOT;
	}

	@Override
	public Material getDisplayItem() {
		return Material.CROSSBOW;
	}

	@Override
	public String getName() {
		return null;
	}
}
