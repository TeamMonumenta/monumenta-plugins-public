package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ArcaneVolatileReactionCS extends VolatileReactionCS {

	public static final String NAME = "Arcane Volatile Reaction";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Carelessly mixing different elements",
			"is the perfect way to create a",
			"catastrophic spectacle.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.FIREWORK_STAR;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public void landEffects(Player player, Plugin plugin, Location loc, double radius) {
		double smallRadiusFactor = 0.3;
		ArcanePotionsCS.drawSimpleAlchemyCircle(
			player,
			loc,
			radius,
			45,
			4,
			smallRadiusFactor,
			ArcanePotionsCS.EARTH,
			Particle.SCRAPE,
			true,
			false);
		ArcanePotionsCS.drawSimpleAlchemyCircle(
			player,
			loc,
			radius,
			0,
			4,
			smallRadiusFactor,
			ArcanePotionsCS.FIRE,
			Particle.WAX_ON,
			true,
			false);
		ArcanePotionsCS.GOLD_SUN.draw(new ArcanePotionsCS.Transform(loc, radius / 2, 0), Particle.WAX_ON, player);
		loc.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1f, 2f);
		loc.getWorld().playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 0.85f, 1.7f);
	}

	@Override
	public void primedMobParticleEffects(Entity mob, Player player, boolean missingBrutalReaction,
										 boolean missingGruesomeReaction, @Nullable AlchemistPotions alchemistPotions) {
		if (missingBrutalReaction) {
			Location loc = LocationUtils.getHalfHeightLocation(mob).add(0, 0.2, 0);
			new PPCircle(Particle.WAX_ON, loc, mob.getWidth())
				.countPerMeter(3)
				.spawnAsPlayerActive(player);
		}
		if (missingGruesomeReaction) {
			Location loc = LocationUtils.getHalfHeightLocation(mob).add(0, -0.2, 0);
			new PPCircle(Particle.SCRAPE, loc, mob.getWidth())
				.countPerMeter(3)
				.spawnAsPlayerActive(player);
		}
	}

	@Override
	public void detonatedMobParticleEffects(Player player, Plugin plugin, LivingEntity mob, boolean isGruesome,
											@Nullable AlchemistPotions alchemistPotions) {
		Vector launchVector =
			new Vector(FastUtils.randomDoubleInRange(0, 1) - 0.5, 1, FastUtils.randomDoubleInRange(0, 1) - 0.5)
				.normalize();

		mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.5f);
		mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1f, 1.75f);
		new BukkitRunnable() {
			int mTicks = 0;
			final int mMaxTicks = FastUtils.randomIntInRange(8, 14);
			final double mDrag = FastUtils.randomDoubleInRange(0.04, 0.06);
			double mMovementSpeed = FastUtils.randomDoubleInRange(0.7, 1);
			final Vector mVel = launchVector;
			final Location mLoc = mob.getEyeLocation();
			Location mPrevVisualLoc = mLoc.clone();

			@Override
			public void run() {
				mTicks++;
				mMovementSpeed -= mDrag;
				mVel.setY(mVel.getY() - mDrag);
				double[] yawPitch = VectorUtils.vectorToRotation(mVel);
				Vector perpendicular = VectorUtils.rotationToVector(yawPitch[0], yawPitch[1] - 90);
				Vector randomOffset = perpendicular.rotateAroundAxis(mVel.normalize(), FastUtils.randomDoubleInRange(0, 2 * Math.PI));
				Location mNewVisualLoc = mLoc.clone().add(randomOffset.multiply(mMovementSpeed));
				mLoc.add(mVel.clone().multiply(mMovementSpeed));

				new PPLine(Particle.ELECTRIC_SPARK, mPrevVisualLoc, mNewVisualLoc)
					.countPerMeter(2)
					.spawnAsPlayerActive(player);
				new PPLine(Particle.ENCHANTMENT_TABLE, mPrevVisualLoc, mNewVisualLoc)
					.countPerMeter(2)
					.spawnAsPlayerActive(player);
				mPrevVisualLoc = mNewVisualLoc;

				if (mTicks >= mMaxTicks || mLoc.getBlock().isSolid()) {
					new PartialParticle(isGruesome ? Particle.SCRAPE : Particle.WAX_ON, mLoc, 70)
						.delta(0.4)
						.spawnAsPlayerActive(player);
					new PartialParticle(Particle.ELECTRIC_SPARK, mLoc, 10)
						.delta(0.2)
						.spawnAsPlayerActive(player);
					mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.PLAYERS, 1f, 1.5f);
					mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1f, 1.5f);

					for (int i = 0; i < FastUtils.randomIntInRange(8, 14); i++) {
						Vector trailLaunchVector = VectorUtils.randomUnitVector();
						new BukkitRunnable() {
							int mTrailTicks = 0;
							final int mTrailMaxTicks = FastUtils.randomIntInRange(4, 7);
							final double mTrailDrag = FastUtils.randomDoubleInRange(0.04, 0.06);
							double mTrailMovementSpeed = FastUtils.randomDoubleInRange(0.5, 0.7);
							final Vector mTrailVel = trailLaunchVector;
							final Location mTrailLoc = mLoc.clone();

							@Override
							public void run() {
								mTrailTicks++;
								mTrailMovementSpeed -= mTrailDrag;
								mTrailVel.setY(mTrailVel.getY() - mTrailDrag);
								mTrailLoc.add(mTrailVel.clone().multiply(mTrailMovementSpeed));
								new PartialParticle(isGruesome ? Particle.SCRAPE : Particle.WAX_ON, mTrailLoc, 1)
									.spawnAsPlayerActive(player);

								if (mTrailTicks >= mTrailMaxTicks || mTrailLoc.getBlock().isSolid()) {
									cancel();
								}
							}
						}.runTaskTimer(plugin, 0, 1);
					}
					cancel();
				}
			}
		}.runTaskTimer(plugin, 0, 1);
	}

	@Override
	public void spreadEnhancementDoT(Player player, LivingEntity from, LivingEntity to) {
		Location fromTop = LocationUtils.getHeightLocation(from, 1);
		Location toTop = LocationUtils.getHeightLocation(to, 1);
		toTop.add(LocationUtils.getDirectionTo(fromTop, toTop).multiply(to.getWidth() / 2));
		Location toElevated = toTop.clone().add(0, 1, 0);
		Location fromElevated = fromTop.clone().add(0, 1, 0);
		if (toElevated.getY() > fromElevated.getY()) {
			fromElevated.setY(toElevated.getY());
		} else {
			toElevated.setY(fromElevated.getY());
		}
		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (mT >= 2) {
					cancel();
					return;
				}
				mT++;

				new PPLine(Particle.END_ROD, fromTop, fromElevated)
					.countPerMeter(2)
					.spawnAsPlayerActive(player);
				new PPLine(Particle.END_ROD, fromElevated, toElevated)
					.countPerMeter(2)
					.spawnAsPlayerActive(player);
				new PPLine(Particle.END_ROD, toElevated, toTop)
					.countPerMeter(2)
					.spawnAsPlayerActive(player);
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 5);
	}

	@Override
	public void enhancementDoTTickParticleEffects(Player player, LivingEntity mob) {
		new PartialParticle(Particle.ELECTRIC_SPARK, LocationUtils.getHalfHeightLocation(mob))
			.count(25)
			.extra(2)
			.spawnAsPlayerActive(player);
	}
}
