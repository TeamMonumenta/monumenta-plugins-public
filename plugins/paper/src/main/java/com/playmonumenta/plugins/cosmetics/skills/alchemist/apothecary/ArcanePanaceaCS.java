package com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.Panacea;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.ArcanePotionsCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPPeriodic;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ArcanePanaceaCS extends PanaceaCS {

	public static final String NAME = "Arcane Panacea";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"While most alchemists strive for the Magnum Opus,",
			"some try to make a name for themselves in medicine.",
			"The Panacea is the ultimate cure for all ailments,",
			"and is equally out of reach as the Philosopher's Stone.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.ENCHANTING_TABLE;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	private static final double CAST_RING_DISTANCE = 1;
	private static final double CAST_RING_RADIUS = 1.15;
	private static final double RADIUS_FOR_DOUBLING = Panacea.PANACEA_RADIUS * 1.75;

	@Override
	public void castEffects(Player player, double radius) {
		World world = player.getWorld();
		Location loc = player.getLocation();
		sounds(world, loc);

		castCircle(player, CAST_RING_RADIUS, CAST_RING_RADIUS * 0.4, false);
		if (radius >= RADIUS_FOR_DOUBLING) {
			castCircle(player, CAST_RING_RADIUS * 2, CAST_RING_RADIUS * 0.6, true);
		}

	}

	private void castCircle(Player player, double radius, double smallRadius, boolean invert) {
		Location loc = player.getLocation();

		// big circle
		Vector up = VectorUtils.rotationToVector(loc.getYaw(), loc.getPitch() - 90);
		Vector right = loc.getDirection().crossProduct(up);
		Location centerLocation = player.getEyeLocation().add(loc.getDirection().multiply(CAST_RING_DISTANCE));
		double arcCut = Math.toDegrees(2 * Math.asin(smallRadius / radius / 2));
		new PPCircle(Particle.ENCHANTMENT_TABLE, centerLocation, radius)
			.axes(right, up).arcDegree(180 + arcCut, 360 - arcCut)
			.includeStart(false).includeEnd(false)
			.countPerMeter(6).spawnAsPlayerActive(player);
		new PPCircle(Particle.ENCHANTMENT_TABLE, centerLocation, radius)
			.axes(right, up).arcDegree(arcCut, 180 - arcCut)
			.includeStart(false).includeEnd(false)
			.countPerMeter(6).spawnAsPlayerActive(player);

		// small circles
		Location fireRingLoc = centerLocation.clone().subtract(right.clone().multiply(radius));
		new PPCircle(Particle.ENCHANTMENT_TABLE, fireRingLoc, smallRadius)
			.axes(right, up)
			.countPerMeter(6).spawnAsPlayerActive(player);
		Location waterRingLoc = centerLocation.clone().add(right.clone().multiply(radius));
		new PPCircle(Particle.ENCHANTMENT_TABLE, waterRingLoc, smallRadius)
			.axes(right, up)
			.countPerMeter(6).spawnAsPlayerActive(player);

		// fire/water symbols in the small circles
		double symbolRadius = smallRadius * 0.8;
		ArcanePotionsCS.FIRE.draw(new ArcanePotionsCS.Transform(invert ? waterRingLoc : fireRingLoc, symbolRadius, 0, right, up), Particle.WAX_ON, player);
		ArcanePotionsCS.WATER.draw(new ArcanePotionsCS.Transform(invert ? fireRingLoc : waterRingLoc, symbolRadius, 0, right, up), Particle.SCRAPE, player);

	}

	@Override
	public void projectileEffects(Player player, Location loc, double radius, int totalTicks, double moveSpeed, Vector increment) {
		totalTicks -= (int) Math.floor(CAST_RING_DISTANCE / moveSpeed);
		if (totalTicks < 0) {
			// Don't show particles behind the ring, just make them appear a bit later.
			// Subtracting from totalTicks ensures that they appear exactly left and right behind the ring and not in some random orientation.
			return;
		}

		if (radius >= RADIUS_FOR_DOUBLING) {
			trailEffect(player, loc, radius / 2, CAST_RING_RADIUS, totalTicks, increment, false);
			trailEffect(player, loc, radius, CAST_RING_RADIUS * 2, totalTicks, increment, true);
		} else {
			trailEffect(player, loc, radius, CAST_RING_RADIUS, totalTicks, increment, false);
		}
	}

	private void trailEffect(Player player, Location loc, double radius, double initialRadius, int totalTicks, Vector increment, boolean invert) {
		// the cast circle is a fixed size, approach the real radius smoothly after casting
		radius -= (radius - initialRadius) * Math.exp(-totalTicks * 0.25);

		// main, coloured particles
		double degrees = totalTicks * 12;
		if (invert) {
			degrees = 180 - degrees;
		}
		Vector vec = new Vector(FastUtils.cosDeg(degrees) * radius, 0, FastUtils.sinDeg(degrees) * radius);
		vec = VectorUtils.rotateXAxis(vec, loc.getPitch() - 90);
		vec = VectorUtils.rotateYAxis(vec, loc.getYaw());
		new PPPeriodic(Particle.WAX_ON, loc.clone().add(vec)).manualTimeOverride(totalTicks).spawnAsPlayerActive(player);
		new PPPeriodic(Particle.SCRAPE, loc.clone().subtract(vec)).manualTimeOverride(totalTicks).spawnAsPlayerActive(player);

		// secondary enchantment particles, one half-step behind
		double degreesHalfStep = degrees + (invert ? 6 : -6);
		Vector vecHalfStep = new Vector(FastUtils.cosDeg(degreesHalfStep) * radius, 0, FastUtils.sinDeg(degreesHalfStep) * radius);
		vecHalfStep = VectorUtils.rotateXAxis(vecHalfStep, loc.getPitch() - 90);
		vecHalfStep = VectorUtils.rotateYAxis(vecHalfStep, loc.getYaw());
		Location locHalfStep = loc.clone().add(increment.clone().multiply(-0.5));
		new PPPeriodic(Particle.ENCHANTMENT_TABLE, locHalfStep.clone().add(vecHalfStep)).manualTimeOverride(totalTicks).spawnAsPlayerActive(player);
		new PPPeriodic(Particle.ENCHANTMENT_TABLE, locHalfStep.clone().subtract(vecHalfStep)).manualTimeOverride(totalTicks).spawnAsPlayerActive(player);
	}

	@Override
	public void projectileReverseEffects(Player player, Location loc, double radius) {
		sounds(loc.getWorld(), loc);

		Vector vec1 = VectorUtils.rotateYAxis(new Vector(1, 0, 0), loc.getYaw());
		Vector vec2 = vec1.getCrossProduct(loc.getDirection());
		new PPCircle(Particle.ENCHANTMENT_TABLE, loc, radius)
			.axes(vec1, vec2)
			.countPerMeter(ArcanePotionsCS.ENCHANT_PARTICLE_PER_METER)
			.spawnAsPlayerActive(player);

		if (radius >= RADIUS_FOR_DOUBLING) {
			new PPCircle(Particle.ENCHANTMENT_TABLE, loc, radius / 2)
				.axes(vec1, vec2)
				.countPerMeter(ArcanePotionsCS.ENCHANT_PARTICLE_PER_METER)
				.spawnAsPlayerActive(player);
		}
	}

	@Override
	public void projectileEndEffects(Player player, Location loc, double radius) {
		for (int i = 0; i < 3; i++) {
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
				loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_FALL, SoundCategory.PLAYERS, 1, 0.5f);
			}, 3L * i);
		}
	}

	@Override
	public void projectileHitEffects(Player player, LivingEntity hitEntity, double radius) {
		if (hitEntity instanceof Player) {
			hitEntity.getWorld().playSound(hitEntity.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1, 2f);
		} else {
			hitEntity.getWorld().playSound(hitEntity.getLocation(), Sound.BLOCK_MEDIUM_AMETHYST_BUD_BREAK, SoundCategory.PLAYERS, 1, 0.5f);
		}
	}

	@Override
	public void damageOverTimeEffects(LivingEntity target) {
		new PartialParticle(Particle.ENCHANTMENT_TABLE, LocationUtils.getHalfHeightLocation(target), 16)
				.delta(target.getBoundingBox().getWidthX() / 3, target.getBoundingBox().getHeight() / 3, target.getBoundingBox().getWidthZ() / 3)
				.spawnAsEnemy();
	}

	private void sounds(World world, Location loc) {
		world.playSound(loc, "minecraft:block.amethyst_block.resonate", SoundCategory.PLAYERS, 1.0f, 1.4f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.5f, 0.6f);
		world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.0f, 1.4f);
		world.playSound(loc, Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.PLAYERS, 1.0f, 1.8f);
	}

}
