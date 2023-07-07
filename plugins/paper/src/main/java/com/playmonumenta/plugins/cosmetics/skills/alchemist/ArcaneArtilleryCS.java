package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPPeriodic;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ArcaneArtilleryCS extends AlchemicalArtilleryCS {

	public static final String NAME = "Arcane Artillery";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Sulphur has many interesting properties,",
			"especially when combined with a bit of charcoal",
			"and saltpeter. Mixing these into a potion",
			"yields even more spectacular results.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.ENCHANTING_TABLE;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public void periodicEffects(Player caster, MagmaCube grenade, Item physicsItem, int ticks) {
		Location cubeCenter = LocationUtils.getHalfHeightLocation(grenade);

		// 4 enchanting particle trails
		Vector forward = physicsItem.getVelocity().normalize();
		if (!Double.isFinite(forward.getX())) {
			return;
		}
		Vector right = forward.getCrossProduct(new Vector(0, 1, 0)).normalize();
		if (!Double.isFinite(right.getX())) {
			return;
		}
		Vector up = right.getCrossProduct(forward);
		right.multiply(grenade.getBoundingBox().getWidthX() / 2);
		up.multiply(grenade.getBoundingBox().getHeight() / 2);
		Location l1 = cubeCenter.clone().add(right).add(up);
		Location l2 = cubeCenter.clone().add(right).subtract(up);
		Location l3 = cubeCenter.clone().subtract(right).add(up);
		Location l4 = cubeCenter.clone().subtract(right).subtract(up);
		new PPPeriodic(Particle.ENCHANTMENT_TABLE, l1).manualTimeOverride(ticks).spawnAsPlayerActive(caster);
		new PPPeriodic(Particle.ENCHANTMENT_TABLE, l2).manualTimeOverride(ticks).spawnAsPlayerActive(caster);
		new PPPeriodic(Particle.ENCHANTMENT_TABLE, l3).manualTimeOverride(ticks).spawnAsPlayerActive(caster);
		new PPPeriodic(Particle.ENCHANTMENT_TABLE, l4).manualTimeOverride(ticks).spawnAsPlayerActive(caster);

	}

	@Override
	public void explosionEffect(Player caster, Location loc, double radius) {

		loc.setDirection(loc.toVector().subtract(caster.getLocation().toVector()));

		World world = loc.getWorld();
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.75f, 0.5f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.75f, 1.25f);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.PLAYERS, 1, 1);
		world.playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.PLAYERS, 2, 0.5f);
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 2, 0.5f);

		// explosion effect
		new PartialParticle(Particle.FLASH, loc).minimumCount(1).spawnAsPlayerActive(caster);
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 30, 0.02, 0.02, 0.02, 0.1 * radius).spawnAsPlayerActive(caster);
		new PartialParticle(Particle.FLAME, loc, 100, radius / 4, 0, radius / 4, 0.04 * radius).spawnAsPlayerActive(caster);

		// lingering smoke
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 50, radius / 2, 0, radius / 2, 0.01).spawnAsPlayerActive(caster);

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				new PartialParticle(Particle.ELECTRIC_SPARK, loc)
					.count(20 - mT)

					.delta(0.4 * radius, 0.5, 0.4 * radius)
					.spawnAsPlayerActive(caster);

				mT++;
				if (mT > 15) {
					cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

		// circle with small circles and fire symbols
		double smallRadiusFactor = 0.3;
		ArcanePotionsCS.drawSimpleAlchemyCircle(caster, loc, radius, 45, 4, smallRadiusFactor, ArcanePotionsCS.FIRE, Particle.WAX_ON, true, false);

		// cross connecting the smaller circles
		double halfLineLength = radius * (1 - smallRadiusFactor);
		new PPLine(Particle.ENCHANTMENT_TABLE,
			loc.clone().add(VectorUtils.rotateYAxis(new Vector(halfLineLength, 0, 0), loc.getYaw() + 45)),
			loc.clone().add(VectorUtils.rotateYAxis(new Vector(halfLineLength, 0, 0), loc.getYaw() + 180 + 45)))
			.countPerMeter(ArcanePotionsCS.ENCHANT_PARTICLE_PER_METER)
			.spawnAsPlayerActive(caster)
			.location(
				loc.clone().add(VectorUtils.rotateYAxis(new Vector(halfLineLength, 0, 0), loc.getYaw() + 90 + 45)),
				loc.clone().add(VectorUtils.rotateYAxis(new Vector(halfLineLength, 0, 0), loc.getYaw() - 90 + 45)))
			.spawnAsPlayerActive(caster);

		// sulphur symbols
		for (int i = 0; i < 4; i++) {
			double rot = loc.getYaw() + i * 90;
			Location l = loc.clone().add(VectorUtils.rotateYAxis(new Vector(radius * 0.6, 0, 0), rot));
			ArcanePotionsCS.SULPHUR.draw(new ArcanePotionsCS.Transform(l, radius * 0.3, rot + 90), Particle.WAX_ON, caster);
		}

	}

	@Override
	public void aftershockEffect(Player caster, Location loc, double radius, List<LivingEntity> hitMobs) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1.5f, 1.3f);
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1.5f, 1.3f);
		double scale = radius * 0.3;

		ArcanePotionsCS.SMALL_SYMBOLS.get(FastUtils.randomIntInRange(0, ArcanePotionsCS.SMALL_SYMBOLS.size() - 1))
			.draw(new ArcanePotionsCS.Transform(loc, scale, 0), Particle.FLAME, caster);

		for (LivingEntity mob : hitMobs) {
			new PPCircle(Particle.ELECTRIC_SPARK, LocationUtils.getHalfHeightLocation(mob), mob.getWidth())
				.ringMode(true).countPerMeter(2).delta(0, LocationUtils.getHeightLocation(mob, 0.25).getY(), 0)
				.extra(0.5).spawnAsPlayerActive(caster);
		}
	}

}
