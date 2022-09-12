package com.playmonumenta.plugins.cosmetics.skills.rogue;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.*;
import org.bukkit.entity.Player;

public class WindStepCS extends AdvancingShadowsCS {
	// Windy advancing shadow. Depth set: wind

	public static final String NAME = "Wind Step";

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName());
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.ADVANCING_SHADOWS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.FEATHER;
	}

	@Override
	public void tpParticle(Player mPlayer) {
		new PartialParticle(Particle.CLOUD, mPlayer.getLocation().add(0, 1.1, 0), 25, 0.35, 0.5, 0.35, 0.1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation().add(0, 1.1, 0), 7, 0.3, 0.5, 0.3, 0.025).spawnAsPlayerActive(mPlayer);

	}

	@Override
	public void tpParticleTrack(Player mPlayer, Location loc) {
		new PartialParticle(Particle.CLOUD, loc.clone().add(0, 1, 0), 3, 0.25, 0.45, 0.25, 0.05).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc.clone().add(0, 1, 0), 4, 0.25, 0.5, 0.25, 0.01).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void tpSound(World world, Player mPlayer) {
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.25f);
	}

	@Override
	public void tpSoundFail(World world, Player mPlayer) {
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.7f, 1.9f);
	}
}
