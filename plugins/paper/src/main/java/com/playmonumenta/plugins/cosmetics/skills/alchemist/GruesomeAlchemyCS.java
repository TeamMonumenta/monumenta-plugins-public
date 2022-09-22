package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;


public class GruesomeAlchemyCS implements CosmeticSkill {

	public static final ImmutableMap<String, GruesomeAlchemyCS> SKIN_LIST = ImmutableMap.<String, GruesomeAlchemyCS>builder()
		.put(GruesomeEchoesCS.NAME, new GruesomeEchoesCS())
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.GRUESOME_ALCHEMY;
	}

	@Override
	public Material getDisplayItem() {
		return Material.DRAGON_BREATH;
	}

	public void effectsOnSwap(Player mPlayer, boolean isGruesomeBeforeSwap) {
		if (isGruesomeBeforeSwap) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1, 1.25f);
		} else {
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1, 0.75f);
		}
	}

	public void particlesOnSplash(Player mPlayer, ThrownPotion mPotion, boolean isGruesome) {

	}

	public float getSwapBrewPitch() {
		return 1.0f;
	}

	public Color splashColor(boolean isGruesome) {
		if (isGruesome) {
			return Color.FUCHSIA;
		} else {
			return Color.BLACK;
		}
	}
}
