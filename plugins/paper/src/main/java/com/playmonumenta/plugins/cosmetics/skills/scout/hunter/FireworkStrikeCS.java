package com.playmonumenta.plugins.cosmetics.skills.scout.hunter;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.*;
import org.bukkit.entity.Player;

public class FireworkStrikeCS extends PredatorStrikeCS {
	//Steely predator stirke. Depth set: steel

	public static final String NAME = "Firework Strike";

	private static final Particle.DustOptions GRAY_COLOR = new Particle.DustOptions(Color.fromRGB(130, 130, 130), 1.0f);
	private static final Particle.DustOptions RED_COLOR = new Particle.DustOptions(Color.fromRGB(244, 56, 0), 1.0f);
	private static final Particle.DustOptions ORANGE_COLOR = new Particle.DustOptions(Color.fromRGB(255, 127, 20), 1.0f);

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName());
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.PREDATOR_STRIKE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.FIREWORK_ROCKET;
	}

	@Override
	public void strikeParticleReady(Player mPlayer) {
		new PartialParticle(Particle.FIREWORKS_SPARK, mPlayer.getLocation().add(0, 0.75, 0), 1, 0.25, 0, 0.25, 0).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void strikeParticleProjectile(Player mPlayer, Location bLoc) {
		new PartialParticle(Particle.FIREWORKS_SPARK, bLoc, 6, 0.15, 0.15, 0.15, 0.125).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIT_MAGIC, bLoc, 10, 0.35, 0.35, 0.35, 0.25).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void strikeParticleExplode(Player mPlayer, Location loc, double radius) {
		new PartialParticle(Particle.SMOKE_LARGE, loc, 20, 0.1 * radius, 0.1 * radius, 0.1 * radius, 0.2).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc, 24, 1.25 * radius, 1.25 * radius, 1.25 * radius, 0.1, GRAY_COLOR).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc, 12, 1.25 * radius, 1.25 * radius, 1.25 * radius, 0.1, RED_COLOR).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc, 8, 1.25 * radius, 1.25 * radius, 1.25 * radius, 0.1, ORANGE_COLOR).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void strikeSoundReady(World world, Player mPlayer) {
		world.playSound(mPlayer.getLocation(), Sound.ITEM_CROSSBOW_LOADING_MIDDLE, 1, 1.25f);
	}

	@Override
	public void strikeSoundLaunch(World world, Player mPlayer) {
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1, 1.0f);
	}

	@Override
	public void strikeSoundExplode(World world, Player mPlayer, Location loc) {
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.25f, 1.25f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.25f, 1.25f);
	}
}
