package com.playmonumenta.plugins.cosmetics.skills.warlock.reaper;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.Material;

public class VoodooBondsCS implements CosmeticSkill {

	public static final ImmutableMap<String, VoodooBondsCS> SKIN_LIST = ImmutableMap.<String, VoodooBondsCS>builder()
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.VOODOO_BONDS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.JACK_O_LANTERN;
	}

	@Override
	public String getName() {
		return null;
	}
}
