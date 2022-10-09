package com.playmonumenta.plugins.cosmetics.skills.rogue.swordsage;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.Material;

public class DeadlyRondeCS implements CosmeticSkill {

	public static final ImmutableMap<String, DeadlyRondeCS> SKIN_LIST = ImmutableMap.<String, DeadlyRondeCS>builder()
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.DEADLY_RONDE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.BLAZE_ROD;
	}

	@Override
	public String getName() {
		return null;
	}
}
