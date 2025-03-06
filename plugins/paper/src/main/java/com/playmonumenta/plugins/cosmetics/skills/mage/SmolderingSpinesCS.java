package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.util.FastMath;
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

public class SmolderingSpinesCS extends MagmaShieldCS {

	public static final String NAME = "Smoldering Spines";
	public static final Color TIP_COLOR = Color.fromRGB(255, 93, 23);
	public static final Color BASE_COLOR = Color.fromRGB(0, 0, 0);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"The abominations of this realm are twisted by",
			"the vile Summer heat, a smoldering, everlasting",
			"hatred seared into their essence.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.POINTED_DRIPSTONE;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public void magmaEffects(World world, Player mPlayer, double radius, double angle) {
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_DEATH, SoundCategory.PLAYERS, 1f, 0.75f);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, SoundCategory.PLAYERS, 1f, 0.75f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.PLAYERS, 1f, 0.5f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.PLAYERS, 1f, 1f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_RAVAGER_STEP, SoundCategory.PLAYERS, 1.5f, 0.5f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.PLAYERS, 1f, 0.5f);

		ParticleUtils.drawParticleCircleExplosion(mPlayer, mPlayer.getLocation().clone().add(0, 0.15, 0).setDirection(mPlayer.getLocation().getDirection().setY(0).normalize()), 0, 1, 0, 0, 40, 0.65f,
			true, 0, 0, Particle.SQUID_INK);
		ParticleUtils.drawParticleCircleExplosion(mPlayer, mPlayer.getLocation().clone().add(0, 0.15, 0).setDirection(mPlayer.getLocation().getDirection().setY(0).normalize()), 0, 1, 0, 0, 25, 0.5f,
			true, 0, 0, Particle.SQUID_INK);


		new BukkitRunnable() {
			final Location mLoc = mPlayer.getLocation();
			double mRadius = 0;
			int mT = 0;

			@Override
			public void run() {
				if (mRadius == 0) {
					mLoc.setDirection(mPlayer.getLocation().getDirection().setY(0).normalize());
				}
				Vector vec;
				mRadius += 1.5;
				double degree = 90 - angle;
				double arcLength = Math.PI * mRadius * ((2 * angle) / 180);
				double divisor = arcLength / Math.max(2, 0.25 * mRadius);
				//degrees advanced each step
				double degreeStep = (2 * angle) / divisor;
				//amount of steps
				int degreeSteps = (int) (((int) (2 * angle)) / degreeStep);
				for (int step = 0; step <= degreeSteps; step++, degree += degreeStep) {
					double radian1 = Math.toRadians(degree);
					vec = new Vector(FastUtils.cos(radian1) * mRadius, 0.125, FastUtils.sin(radian1) * mRadius);
					vec = VectorUtils.rotateXAxis(vec, mLoc.getPitch());
					vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

					Location l = mLoc.clone().add(0, 0.1, 0).add(vec);
					//mLoc is the player's location, l is the location of the spike, pre offset
					Vector toPlayer = VectorUtils.rotateTargetDirection(l.toVector().subtract(mLoc.clone().toVector()), 0, -82);

					Vector dir = VectorUtils.randomUnitVector().multiply(0.25).add(toPlayer).setY(2).normalize();
					Location offsetLoc = LocationUtils.varyInUniform(l, 0.25, 0, 0.25).clone().add(dir.multiply(Math.min((0.25 * (mRadius * 0.6)) * FastUtils.randomDoubleInRange(0.8, 1.2), 1.25)));
					drawLineSlash(offsetLoc, dir, 0, Math.min(0.75 + 0.2 * mRadius * FastUtils.randomDoubleInRange(0.8, 1.2), 1.8), 0.18, 3, (Location lineLoc, double middleProgress, double endProgress, boolean middle) ->
						new PartialParticle(Particle.REDSTONE, lineLoc, 1, 0, 0, 0, 0, new Particle.DustOptions(
							ParticleUtils.getTransition(BASE_COLOR, TIP_COLOR, Math.pow(endProgress, 1.5)), Math.max(1.3f, 3f - (float) (endProgress * 1.4f))))
							.spawnAsPlayerActive(mPlayer));

					if (FastUtils.randomIntInRange(1, 2) % 2 == 0) {
						new PartialParticle(Particle.LAVA, offsetLoc, 1, 0.15, 0.15, 0.15, 0.15).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.SMALL_FLAME, offsetLoc, 1, 0.15, 0.15, 0.15, 0.2 * FastUtils.randomDoubleInRange(0.8, 1.2)).directionalMode(true).delta(FastUtils.randomDoubleInRange(-0.25, 0.25), FastUtils.randomDoubleInRange(0.85, 1.25), FastUtils.randomDoubleInRange(-0.25, 0.25)).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.SMOKE_NORMAL, offsetLoc, 1, 0.15, 0.15, 0.15, 0.1).spawnAsPlayerActive(mPlayer);
					}

				}

				if (mRadius >= radius) {
					this.cancel();
				}
				mT++;
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);

	}

	public static void drawLineSlash(Location loc, Vector dir, double angle, double length, double spacing, int duration, ParticleUtils.LineSlashAnimation animation) {
		Location l = loc.clone();
		l.setDirection(dir);

		List<Vector> points = new ArrayList<>();
		Vector vec = new Vector(0, 0, 1);
		vec = VectorUtils.rotateZAxis(vec, angle);
		vec = VectorUtils.rotateXAxis(vec, l.getPitch());
		vec = VectorUtils.rotateYAxis(vec, l.getYaw());
		vec = vec.normalize();

		for (double ln = -length; ln < length; ln += spacing) {
			Vector point = l.toVector().add(vec.clone().multiply(ln));
			points.add(point);
		}

		if (duration <= 0) {
			boolean midReached = false;
			for (int i = 0; i < points.size(); i++) {
				Vector point = points.get(i);
				boolean middle = !midReached && i == points.size() / 2;
				if (middle) {
					midReached = true;
				}
				animation.lineSlashAnimation(point.toLocation(loc.getWorld()),
					1D - (point.distance(l.toVector()) / length), (double) (i + 1) / points.size(), middle);
			}
		} else {
			new BukkitRunnable() {
				final int mPointsPerTick = (int) (points.size() * (1D / duration));
				int mT = 0;
				boolean mMidReached = false;

				@Override
				public void run() {


					for (int i = mPointsPerTick * mT; i < FastMath.min(points.size(), mPointsPerTick * (mT + 1)); i++) {
						Vector point = points.get(i);
						boolean middle = !mMidReached && i == points.size() / 2;
						if (middle) {
							mMidReached = true;
						}
						animation.lineSlashAnimation(point.toLocation(loc.getWorld()),
							1D - (point.distance(l.toVector()) / length), (double) (i + 1) / points.size(), middle);
					}
					mT++;

					if (mT >= duration) {
						this.cancel();
					}
				}

			}.runTaskTimer(Plugin.getInstance(), 0, 1);
		}
	}
}
