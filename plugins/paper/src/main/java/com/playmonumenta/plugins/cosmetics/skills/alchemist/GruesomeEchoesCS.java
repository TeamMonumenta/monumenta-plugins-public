package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public class GruesomeEchoesCS extends GruesomeAlchemyCS {
	//Twisted theme

	public static final String NAME = "Gruesome Echoes";

	private static final Particle.DustOptions TWIST_COLOR = new Particle.DustOptions(Color.fromRGB(120, 0, 40), 1.0f);
	private static final Particle.DustOptions ECHO_COLOR = new Particle.DustOptions(Color.fromRGB(64, 64, 96), 1.0f);

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

	@Override
	public void particleOnSwap(Player mPlayer, boolean isGruesomeBeforeSwap) {
		if (!isGruesomeBeforeSwap) { // brutal -> gruesome, dark red
			new PartialParticle(Particle.REDSTONE, mPlayer.getLocation().clone().add(0, 1, 0), 30, 0.25, 0.5, 0.25, 0.05, GruesomeEchoesCS.TWIST_COLOR).spawnAsPlayerActive(mPlayer);
		} else { // gruesome -> brutal, darker blue
			new PartialParticle(Particle.REDSTONE, mPlayer.getLocation().clone().add(0, 1, 0), 30, 0.25, 0.5, 0.25, 0.05, GruesomeEchoesCS.ECHO_COLOR).spawnAsPlayerActive(mPlayer);
		}
		new PartialParticle(Particle.SOUL, mPlayer.getLocation().clone().add(0, 1, 0), 5, 0.25, 0.5, 0.25, 0).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public float getSwapBrewPitch() {
		return 0.7f;
	}
}
