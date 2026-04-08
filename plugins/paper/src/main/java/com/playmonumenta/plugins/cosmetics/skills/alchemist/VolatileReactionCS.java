package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
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

public class VolatileReactionCS implements CosmeticSkill {
	private static final Color EFFECT_RING_BROWN = Color.fromRGB(165, 103, 41);
	private static final Color ENHANCEMENT_DOT_TICK_ORANGE = Color.fromRGB(224, 92, 26);
	private static final Particle.DustOptions ENHANCEMENT_DOT_TICK_ORANGE_OPTIONS = new Particle.DustOptions(ENHANCEMENT_DOT_TICK_ORANGE, 1.5f);

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.VOLATILE_REACTION;
	}

	@Override
	public Material getDisplayItem() {
		return Material.FIRE_CHARGE;
	}

	public void landEffects(Player player, Plugin plugin, Location loc, double radius) {
		Location slightlyElevatedLoc = loc.clone().add(0, 0.1, 0);
		new PPCircle(Particle.FLAME, slightlyElevatedLoc, radius)
			.delta(-1, 0, 0)
			.extra(0.5)
			.rotateDelta(true)
			.directionalMode(true)
			.countPerMeter(2)
			.ringMode(false)
			.spawnAsPlayerActive(player);
		new PPCircle(Particle.REDSTONE, slightlyElevatedLoc, radius)
			.ringMode(true)
			.countPerMeter(2)
			.data(new Particle.DustOptions(EFFECT_RING_BROWN, 1.5f))
			.spawnAsPlayerActive(player);
		new PPCircle(Particle.REDSTONE, slightlyElevatedLoc, radius)
			.ringMode(false)
			.count(150)
			.data(new Particle.DustOptions(EFFECT_RING_BROWN, 1.5f))
			.spawnAsPlayerActive(player);
		new PPCircle(Particle.EXPLOSION_LARGE, slightlyElevatedLoc, radius)
			.count((int) (radius * 2))
			.ringMode(false)
			.spawnAsPlayerActive(player);

		loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1f, 0.75f);
		loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.75f);
		Bukkit.getScheduler().runTaskLater(
			plugin,
			() -> loc.getWorld().playSound(loc, Sound.BLOCK_BREWING_STAND_BREW, 1.5f, 0.75f),
			2
		);
	}

	public void primedMobParticleEffects(Entity mob, Player player, boolean missingBrutalReaction,
										 boolean missingGruesomeReaction, @Nullable AlchemistPotions alchemistPotions) {
		if (missingBrutalReaction) {
			Particle.DustOptions brutalDustOptions = new Particle.DustOptions(
				alchemistPotions != null ? alchemistPotions.mCosmetic.getBrutalColor() : GruesomeAlchemyCS.BRUTAL_COLOR,
				1.5f
			);
			new PartialParticle(Particle.REDSTONE, LocationUtils.getHalfHeightLocation(mob), 24)
				.delta(0.2, 0.4, 0.2)
				.data(brutalDustOptions)
				.spawnAsPlayerActive(player);
		}
		if (missingGruesomeReaction) {
			Particle.DustOptions gruesomeDustOptions = new Particle.DustOptions(
				alchemistPotions != null ? alchemistPotions.mCosmetic.getGruesomeColor() : GruesomeAlchemyCS.GRUESOME_COLOR,
				1.5f
			);
			new PartialParticle(Particle.REDSTONE, LocationUtils.getHalfHeightLocation(mob), 24)
				.delta(0.2, 0.4, 0.2)
				.data(gruesomeDustOptions)
				.spawnAsPlayerActive(player);
		}
	}

	public void detonatedMobParticleEffects(Player player, Plugin plugin, LivingEntity mob, boolean isGruesome,
											@Nullable AlchemistPotions alchemistPotions) {
		Particle.DustOptions fireworkDustOptions;
		if (isGruesome) {
			fireworkDustOptions = new Particle.DustOptions(
				alchemistPotions != null ? alchemistPotions.mCosmetic.getGruesomeColor() : GruesomeAlchemyCS.GRUESOME_COLOR,
				1.2f
			);
		} else {
			fireworkDustOptions = new Particle.DustOptions(
				alchemistPotions != null ? alchemistPotions.mCosmetic.getBrutalColor() : GruesomeAlchemyCS.BRUTAL_COLOR,
				1.2f
			);
		}
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

			@Override
			public void run() {
				mTicks++;
				mMovementSpeed -= mDrag;
				mVel.setY(mVel.getY() - mDrag);
				mLoc.add(mVel.clone().multiply(mMovementSpeed));
				new PartialParticle(Particle.CRIT, mLoc, 1)
					.minimumCount(1)
					.spawnAsPlayerActive(player);

				if (mTicks >= mMaxTicks || mLoc.getBlock().isSolid()) {
					new PartialParticle(Particle.REDSTONE, mLoc, 70)
						.delta(0.4)
						.data(fireworkDustOptions)
						.spawnAsPlayerActive(player);
					new PartialParticle(Particle.ELECTRIC_SPARK, mLoc, 10)
						.delta(0.2)
						.spawnAsPlayerActive(player);
					mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.PLAYERS, 1f, 1.5f);
					mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1f, 1.5f);

					Particle.DustOptions trailDustOptions = new Particle.DustOptions(fireworkDustOptions.getColor(), 0.75f);
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
								new PartialParticle(Particle.REDSTONE, mTrailLoc, 1)
									.data(trailDustOptions)
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

		new PartialParticle(Particle.FLAME, LocationUtils.getHalfHeightLocation(mob))
			.count(5)
			.delta(0.5)
			.extra(0.1)
			.spawnAsPlayerActive(player);
		new PartialParticle(Particle.LAVA, LocationUtils.getHalfHeightLocation(mob))
			.count(5)
			.delta(0.5)
			.extra(0.25)
			.spawnAsPlayerActive(player);
		new PartialParticle(Particle.TOTEM, LocationUtils.getHalfHeightLocation(mob))
			.count(10)
			.delta(0.5)
			.extra(0.5)
			.spawnAsPlayerActive(player);
	}

	public void spreadEnhancementDoT(Player player, LivingEntity from, LivingEntity to) {
		ParticleUtils.launchOrb(
			new Vector(FastUtils.randomDoubleInRange(0, 0.5), 1, FastUtils.randomDoubleInRange(0, 0.5)).normalize(),
			LocationUtils.getHalfHeightLocation(from),
			player,
			to,
			100,
			null,
			new Particle.DustOptions(ParticleUtils.getRandomCloseColor(ENHANCEMENT_DOT_TICK_ORANGE, 30), 0.8f),
			(mob) -> {}
		);
	}

	public void enhancementDoTTickParticleEffects(Player player, LivingEntity mob) {
		new PartialParticle(Particle.FLAME, LocationUtils.getHalfHeightLocation(mob))
			.count(15)
			.delta(0.5)
			.extra(0.1)
			.spawnAsPlayerActive(player);
		new PartialParticle(Particle.LAVA, LocationUtils.getHalfHeightLocation(mob))
			.count(15)
			.delta(0.5)
			.extra(0.25)
			.spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, LocationUtils.getHalfHeightLocation(mob))
			.count(10)
			.delta(1)
			.data(ENHANCEMENT_DOT_TICK_ORANGE_OPTIONS)
			.spawnAsPlayerActive(player);
	}
}
