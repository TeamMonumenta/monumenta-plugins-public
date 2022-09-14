package com.playmonumenta.plugins.cosmetics.skills.rogue;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.*;
import org.bukkit.entity.Player;

public class TranscCombosCS extends ViciousCombosCS {
	//Twisted theme

	public static final String NAME = "Transcendent Combos";

	private static final Particle.DustOptions TRANSC_COLOR = new Particle.DustOptions(Color.fromRGB(160, 224, 255), 1.0f);

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName());
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.VICIOUS_COMBOS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.WITHER_SKELETON_SKULL;
	}

	@Override
	public void comboOnKill(World world, Location loc, Player mPlayer, double range) {
		final double VICIOUS_COMBOS_RANGE = range;
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.4f);
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 4f, 0.3f);
		new PartialParticle(Particle.REDSTONE, loc, 80, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, 0.25, TRANSC_COLOR).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 40, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, 0.1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SWEEP_ATTACK, loc, 30, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, 0.001).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, loc, 20, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, 0.001).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void comboOnElite(World world, Location loc, Player mPlayer, double range) {
		final double VICIOUS_COMBOS_RANGE = range;
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 2f, 0.4f);
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 8f, 0.3f);
		new PartialParticle(Particle.REDSTONE, loc, 800, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, 0.25, TRANSC_COLOR).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 400, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, 0.1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SWEEP_ATTACK, loc, 350, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, 0.001).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, loc, 250, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, 0.001).spawnAsPlayerActive(mPlayer);
	}
}
