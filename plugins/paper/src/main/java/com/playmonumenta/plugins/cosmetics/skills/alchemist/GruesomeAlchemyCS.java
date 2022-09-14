package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.Material;
import org.bukkit.entity.Player;



public class GruesomeAlchemyCS implements CosmeticSkill {

	public static final ImmutableMap<String, GruesomeAlchemyCS> SKIN_LIST = ImmutableMap.<String, GruesomeAlchemyCS>builder()
		.put(GruesomeEchoesCS.NAME, new GruesomeEchoesCS())
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, GruesomeEchoesCS.NAME, false, this.getAbilityName());
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.GRUESOME_ALCHEMY;
	}

	@Override
	public Material getDisplayItem() {
		return Material.DRAGON_BREATH;
	}

	public void particleOnSwap(Player mPlayer, boolean isGruesomeBeforeSwap) {
		//Nope!
	}

	public float getSwapBrewPitch() {
		return 1.0f;
	}
}
