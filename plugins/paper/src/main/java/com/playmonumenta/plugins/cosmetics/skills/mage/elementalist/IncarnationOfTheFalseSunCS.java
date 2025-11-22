package com.playmonumenta.plugins.cosmetics.skills.mage.elementalist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class IncarnationOfTheFalseSunCS extends StarfallCS {
	public static final String NAME = "Incarnation of the False Sun";
	private static final Color LIGHTER = Color.fromRGB(252, 217, 119);
	private static final Color LIGHT = Color.fromRGB(255, 216, 0);
	private static final Color ORANGE_RED = Color.fromRGB(232, 46, 0);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Flame born of hatred and agony coalesces into clumps",
			"of seething, roiling chaos. To the denizens of this",
			"realm, they are vestiges of the sun's incandescence."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.SHROOMLIGHT;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.STARFALL;
	}

	@Override
	public void starfallCastEffect(World world, Player player, Location loc) {
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1.2f, 0.5f);
		world.playSound(loc, Sound.ENTITY_ALLAY_DEATH, SoundCategory.PLAYERS, 1.2f, 0.5f);
		new PartialParticle(Particle.LAVA, loc, 15, 0.25f, 0.1f, 0.25f).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FLAME, loc, 30, 0.25f, 0.1f, 0.25f, 0.15f).spawnAsPlayerActive(player);
	}

	@Override
	public void starfallCastTrail(Location loc, Player player) {
	}


	@Override
	public void starfallFallEffect(World world, Player player, Location loc, Location ogLoc, Location ogMeteorLoc, double tick) {
		Location center = ogLoc.clone().add(0, 5, 0);
		Location l = ogLoc.clone().add(0, 5 - (0.9 * Math.min(1, 0.1 * tick)), 0);
		l.setDirection(l.getDirection().setY(0).normalize());
		if (tick % 2 == 0) {
			float initialRotation = loc.getYaw() + FastUtils.randomIntInRange(90, 270);
			double rotPerTick = 2;
			double rotation = rotPerTick * 50 / 2 - rotPerTick * (50 - tick) / 2;


			for (double i = 0.01; i < 1.8; i += 0.089) {
				Location circLoc = l.clone().add(0, i * Math.min(1, 0.1 * tick), 0);
				double radius = Math.min(1, 0.1 * tick) * Math.sqrt(0.81 - Math.pow(i - 0.9, 2));
				new PPCircle(Particle.SMALL_FLAME, circLoc, radius)
					.arcDegree(initialRotation + rotation + FastUtils.randomIntInRange(-30, 30), initialRotation + rotation + 360)
					.countPerMeter(0.5)
					.directionalMode(true)
					.rotateDelta(true)
					.delta(FastUtils.randomDoubleInRange(-0.4, -0.2), FastUtils.randomDoubleInRange(-0.2, 0.2), 1)
					.extra(Math.toRadians(rotPerTick) * radius * FastUtils.randomDoubleInRange(1.2, 1.6))
					.spawnAsPlayerActive(player);
			}

			new PartialParticle(Particle.LAVA, center, 5, 0.15, 0, 0.15, 0)
				.minimumCount(1).spawnAsPlayerActive(player);
			if (tick >= 6) {
				new PartialParticle(Particle.REDSTONE, center, Math.min((int) (tick * 10), 50), 0.32 * Math.min(1, 0.1 * tick), 0.32 * Math.min(1, 0.3 * tick), 0.32 * Math.min(1, 0.1 * tick), 0, new Particle.DustOptions(
					LIGHTER, 1.1f))
					.spawnAsPlayerActive(player);
				for (int i = 0; i < 3; i++) {
					Vector dir = VectorUtils.randomUnitVector().multiply(FastUtils.randomDoubleInRange(1, 1.5));
					spawnTendril(center, center.clone().add(dir.multiply(2.25 * Math.min(1, 0.1 * tick))), player, LIGHT, ORANGE_RED);
				}
			}
			world.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 0.5f, 0.5f + (float) (0.05 * tick));
		}

		if (tick % 10 == 0 || tick == 1) {
			new PPCircle(Particle.REDSTONE, ogMeteorLoc.clone().add(0, 0.25, 0), 1)
				// 1 particle per 6 degrees; 30 particles per pi radians.
				// 1 radian per radius meters circumference
				// 30/(pi * radius) particles per meter
				.countPerMeter(30.0 / Math.PI).extra(0)
				.data(new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1)).distanceFalloff(15)
				.spawnAsBoss();
			new PPCircle(Particle.FLAME, ogMeteorLoc.clone().add(0, 0.25, 0), 1)
				// RotateDelta originates from positive X
				.delta(1, 0, 0).rotateDelta(true)
				// 1 particle per 2 degrees; 90 particles per pi radians.
				// 1 radian per radius meters circumference
				// 90/(pi * radius) particles per meter
				.countPerMeter(90.0 / Math.PI).extra(0.15).directionalMode(true).distanceFalloff(15)
				.spawnAsPlayerActive(player);
		}

	}

	@Override
	public void starfallLandEffect(World world, Player player, Location loc, Location ogLoc, double radius) {
		Location sunLoc = ogLoc.clone().add(0, 5, 0);
		new PPParametric(Particle.FLAME, sunLoc, (parameter, builder) -> {
			Vector vector = VectorUtils.randomUnitVector();
			builder.location(sunLoc.clone().add(vector.clone()));
			builder.offset(-vector.getX(), -vector.getY(), -vector.getZ());
		}).directionalMode(true).count(200).extra(0.35).spawnAsPlayerActive(player);

		world.playSound(sunLoc, Sound.BLOCK_CONDUIT_DEACTIVATE, SoundCategory.PLAYERS, 1.2f, 0.5f);
		world.playSound(sunLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 1.2f, 0.5f);
		world.playSound(sunLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.2f, 0.5f);

		world.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.PLAYERS, 0.8f, 1f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0f, 0.5f);
		world.playSound(loc, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 1.0f, 0.5f);
		world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 2.0f, 0.5f);

		new BukkitRunnable() {
			final Location mL = sunLoc;
			final Location mDestination = loc;
			int mT = 0;
			int mIter = 0;
			double mAccelStrength = 0.2;
			boolean mHasBumpedUp = false;
			final Vector mVelocity = VectorUtils.rotateTargetDirection(loc.toVector().subtract(sunLoc.toVector()), 0, -60);

			double mYawPush = 0;
			double mPitchPush = 0;

			@Override
			public void run() {
				mT++;

				for (int i = 0; i < 10; i++) {
					mIter++;

					mVelocity.add(LocationUtils.getDirectionTo(mDestination, mL).multiply(mAccelStrength));

					// every once in a while, add some random force to the path
					if (mIter % 20 == 0) {
						mYawPush = (FastUtils.RANDOM.nextBoolean() ? 1 : -1) * FastUtils.randomDoubleInRange(30, 80);
						mPitchPush = FastUtils.randomDoubleInRange(-30, 10);
					}
					if (mIter % 20 < 12 && !mHasBumpedUp) {
						mVelocity.add(VectorUtils.rotateTargetDirection(LocationUtils.getDirectionTo(mDestination, mL),
							mYawPush, mPitchPush).multiply(0.22));
					}

					if (mVelocity.length() > 0.27) {
						mVelocity.normalize().multiply(0.27);
					}

					mL.add(mVelocity);

					new PartialParticle(Particle.REDSTONE, mL, 1, 0.05, 0.05, 0.05, 0,
						new Particle.DustOptions(LIGHTER, 1.5f))
						.spawnAsPlayerActive(player);
					new PartialParticle(Particle.REDSTONE, mL, 1, 0.05, 0.05, 0.05, 0,
						new Particle.DustOptions(LIGHT, 1.5f))
						.spawnAsPlayerActive(player);
					if (mIter % 2 == 0) {
						new PartialParticle(Particle.SMALL_FLAME, mL, 1, 0, 0, 0, 0.05).spawnAsPlayerActive(player);
					}

					// stop random motion and increase acceleration strength once we get close enough
					if (mL.distance(mDestination) < 5 && !mHasBumpedUp) {
						mHasBumpedUp = true;
						mVelocity.add(new Vector(0, 3.5, 0));
						mAccelStrength = 0.07;
					}
					if (mHasBumpedUp) {
						mAccelStrength += 0.01;
					}

					if (mL.distance(mDestination) < 0.5) {
						ParticleUtils.drawParticleCircleExplosion(player, mDestination.clone().add(0, 0.05, 0), 0, 0.5, -mDestination.getYaw(), -mDestination.getPitch(), 30,
							0.15f, true, 0, 0, Particle.FLAME);
						this.cancel();
						new PartialParticle(Particle.FLAME, loc, 75).delta(0.5).extraRange(0.35, 0.6).spawnAsPlayerActive(player);
						new PartialParticle(Particle.LAVA, loc, 100).delta(radius / 2).spawnAsPlayerActive(player);

						new PPCircle(Particle.FLAME, loc, 2)
							.count(50).randomizeAngle(true)
							.spawnAsPlayerActive(player);
						new PPCircle(Particle.FLAME, loc, 1.8)
							.count(50).randomizeAngle(true)
							.delta(0, 1, 0).directionalMode(true).extraRange(0.0, 0.1)
							.spawnAsPlayerActive(player);
						new PPCircle(Particle.FLAME, loc, 1.6)
							.count(120).randomizeAngle(true)
							.delta(0, 1, 0).directionalMode(true).extraRange(0.1, 0.4)
							.spawnAsPlayerActive(player);
						new PPCircle(Particle.FLAME, loc, 1.4)
							.count(240).randomizeAngle(true)
							.delta(0, 1, 0).directionalMode(true).extraRange(0.3, 0.65)
							.spawnAsPlayerActive(player);
						new PPCircle(Particle.SMALL_FLAME, loc, 1.3)
							.count(150).randomizeAngle(true)
							.delta(0, 1, 0).directionalMode(true).extraRange(0.3, 0.9)
							.spawnAsPlayerActive(player);
						new PPCircle(Particle.SMALL_FLAME, loc, 1.2)
							.count(60).randomizeAngle(true)
							.delta(0, 1, 0).directionalMode(true).extraRange(0.85, 1.45)
							.spawnAsPlayerActive(player);

						new PPCircle(Particle.END_ROD, loc, 1.1)
							.count(50).randomizeAngle(true)
							.spawnAsPlayerActive(player);
						new PPCircle(Particle.END_ROD, loc, 1)
							.count(50).randomizeAngle(true)
							.delta(0, 1, 0).directionalMode(true).extraRange(0.0, 0.1)
							.spawnAsPlayerActive(player);
						new PPCircle(Particle.END_ROD, loc, 1)
							.count(60).randomizeAngle(true)
							.delta(0, 1, 0).directionalMode(true).extraRange(0.1, 0.4)
							.spawnAsPlayerActive(player);
						new PPCircle(Particle.END_ROD, loc, 1)
							.count(120).randomizeAngle(true)
							.delta(0, 1, 0).directionalMode(true).extraRange(0.3, 0.65)
							.spawnAsPlayerActive(player);
						new PPCircle(Particle.END_ROD, loc, 1)
							.count(50).randomizeAngle(true)
							.delta(0, 1, 0).directionalMode(true).extraRange(0.3, 0.9)
							.spawnAsPlayerActive(player);
						new PPCircle(Particle.END_ROD, loc, 1)
							.count(60).randomizeAngle(true)
							.delta(0, 1, 0).directionalMode(true).extraRange(0.85, 1.45)
							.spawnAsPlayerActive(player);

						new PPCircle(Particle.LAVA, loc, radius * 0.9)
							.count(40).randomizeAngle(true)
							.spawnAsPlayerActive(player);
						return;
					}
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	private void spawnTendril(Location loc, Location to, Player mPlayer, Color color1, Color color2) {
		double distance = loc.distance(to);
		Vector dirTo = to.toVector().subtract(loc.toVector());

		new BukkitRunnable() {
			final Location mL = loc.clone();
			final double mXMult = FastUtils.randomDoubleInRange(-0.7, 0.7);
			final double mZMult = FastUtils.randomDoubleInRange(-0.7, 0.7);
			final Vector mVecStep = dirTo.normalize().multiply(0.15);

			@Override
			public void run() {
				for (int i = 0; i < distance * 1.25; i++) {
					float size = 0.75f + (1.5f * (float) (1 - (mL.distance(loc) / distance)));
					double offset = 0.1 * (1f - (mL.distance(loc) / distance));
					double transition = (mL.distance(loc) / distance);
					double pi = (Math.PI * 2) * Math.max((1f - (mL.distance(loc) / distance)), 0);


					Vector vec = new Vector(mXMult * FastUtils.cos(pi), 0,
						mZMult * FastUtils.sin(pi));
					vec = VectorUtils.rotateTargetDirection(vec, loc.getYaw(), loc.getPitch() + 90);
					Location tendrilLoc = mL.clone().add(vec);

					new PartialParticle(Particle.REDSTONE, tendrilLoc, 2, offset, offset, offset, 0, new Particle.DustOptions(
						ParticleUtils.getTransition(color1, color2, transition), size))
						.spawnAsPlayerActive(mPlayer);
					mL.add(mVecStep);
					if (mL.distance(to) < 0.1) {
						this.cancel();
						return;
					}
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
