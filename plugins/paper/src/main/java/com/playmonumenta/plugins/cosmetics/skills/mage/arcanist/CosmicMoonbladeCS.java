package com.playmonumenta.plugins.cosmetics.skills.mage.arcanist;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.Material;

public class CosmicMoonbladeCS implements CosmeticSkill {

	public static final ImmutableMap<String, CosmicMoonbladeCS> SKIN_LIST = ImmutableMap.<String, CosmicMoonbladeCS>builder()
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.COSMIC_MOONBLADE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.DIAMOND_SWORD;
	}

	@Override
	public String getName() {
		return null;
	}
}
