package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.*;
import org.bukkit.entity.Player;

public class VolcanicBurstCS extends MagmaShieldCS {
	// Meteor like magma shield. Depth set: flame
	// It used to be Heavenly Blast. I deeply loved it and a song with the same name.

	public static final String NAME = "Volcanic Burst";

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, VolcanicBurstCS.NAME, false, this.getAbilityName());
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.MAGMA_SHIELD;
	}

	@Override
	public Material getDisplayItem() {
		return Material.MAGMA_BLOCK;
	}

	@Override
	public void magmaParticle(Player mPlayer, Location l) {
		new PartialParticle(Particle.FLAME, l, 2, 0.15, 0.15, 0.15, 0.125).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SOUL_FIRE_FLAME, l, 2, 0.15, 0.15, 0.15, 0.15).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_LARGE, l, 1, 0.05, 0.05, 0.05, 0.075).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void magmaSound(World world, Player mPlayer) {
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.1f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.75f, 0f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.6f);
	}

}
