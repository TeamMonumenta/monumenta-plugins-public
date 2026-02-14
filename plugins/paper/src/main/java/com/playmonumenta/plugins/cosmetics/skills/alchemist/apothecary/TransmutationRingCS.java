package com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class TransmutationRingCS implements CosmeticSkill {

	private static final Particle.DustOptions GOLD_COLOR = new Particle.DustOptions(Color.fromRGB(255, 200, 0), 1.2f);
	private static final int MAX_COLOR_OFFSET = 50;

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.TRANSMUTATION_RING;
	}

	@Override
	public Material getDisplayItem() {
		return Material.GLOWSTONE_DUST;
	}

	public void startEffect(Player player, Location center, double radius) {
		center.getWorld().playSound(center, Sound.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS, 3f, 0.35f);
	}

	private double getInstabilityPercent(int killCount, int maxKills) {
		return (double) killCount / (double) maxKills;
	}

	public void periodicEffect(Player player, Location center, double radius, int tick, int maxTicks, int maximumPotentialTicks, int killCount, int maxKills) {
		PPCircle particles = new PPCircle(Particle.REDSTONE, center, radius).data(GOLD_COLOR);
		particles.count((int) Math.floor(120 * radius / 5)).location(center.clone().add(0, 0.25, 0)).spawnAsPlayerActive(player);
		particles.count((int) Math.floor(30 * radius / 5)).location(center.clone().add(0, 0.75, 0)).spawnAsPlayerActive(player);
		particles.count((int) Math.floor(15 * radius / 5)).location(center.clone().add(0, 1.25, 0)).spawnAsPlayerActive(player);
		particles.count((int) Math.floor(7 * radius / 5)).location(center.clone().add(0, 2, 0)).spawnAsPlayerActive(player);

		int offset = (int) (getInstabilityPercent(killCount, maxKills) * MAX_COLOR_OFFSET);
		double sizeCoefficient = (double) killCount / (double) maxKills;
		Particle.DustOptions dustOptions = new Particle.DustOptions(ParticleUtils.getOffsetColor(GOLD_COLOR.getColor(), -offset), 1.2f);
		Location centerElevated = center.clone().add(0, 0.1, 0);
		// Center circle
		new PPCircle(Particle.REDSTONE, centerElevated, 0.75)
			.data(dustOptions)
			.countPerMeter(4)
			.ringMode(false)
			.spawnAsPlayerActive(player);
		// Expanding ring
		if (sizeCoefficient > 0) {
			new PPCircle(Particle.REDSTONE, centerElevated, radius * sizeCoefficient)
				.data(dustOptions)
				.countPerMeter(2)
				.ringMode(true)
				.spawnAsPlayerActive(player);
		}
		// Random particles in the center
		new PPCircle(Particle.REDSTONE, centerElevated, radius)
			.data(GOLD_COLOR)
			.count(30)
			.ringMode(false)
			.spawnAsPlayerActive(player);
	}

	public void effectOnKill(Player player, Location loc, Location centerLoc, int killCount, int maxKills, boolean isElite) {
		int offset = (int) (getInstabilityPercent(killCount, maxKills) * MAX_COLOR_OFFSET);
		player.getWorld().playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 0.7f, 2);
		ParticleUtils.launchOrb(
			new Vector(FastUtils.randomDoubleInRange(0, 0.5), 1, FastUtils.randomDoubleInRange(0, 0.5)).normalize(),
			loc,
			player,
			player,
			100,
			centerLoc.clone().add(0, -0.5, 0),
			new Particle.DustOptions(ParticleUtils.getOffsetColor(GOLD_COLOR.getColor(), -offset), isElite ? 3f : 1f),
			(mob) -> { }
		);
	}

	public void explode(Player player, Location centerLoc, double radius) {
		centerLoc.getWorld().playSound(centerLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.5f, 0.75f);
		centerLoc.getWorld().playSound(centerLoc, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.5f, 0.5f);
		centerLoc.getWorld().playSound(centerLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 1.5f, 0.5f);
		PPCircle circle = new PPCircle(Particle.END_ROD, centerLoc, radius)
			.ringMode(true)
			.directionalMode(true)
			.delta(0, 1, 0)
			.countPerMeter(2);
		circle.extra(0.25).spawnAsPlayerActive(player);
		circle.extra(-0.25).spawnAsPlayerActive(player);
		circle.extra(0.5).spawnAsPlayerActive(player);
		circle.extra(-0.5).spawnAsPlayerActive(player);
		circle.extra(0.75).spawnAsPlayerActive(player);
		circle.extra(-0.75).spawnAsPlayerActive(player);
	}

}
