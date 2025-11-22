package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class GraspOfWinterCS extends FrostNovaCS {
	public static final String NAME = "Grasp of Winter";
	public static final Color TIP_COLOR = Color.fromRGB(145, 200, 255);
	public static final Color BASE_COLOR = Color.fromRGB(44, 44, 89);
	private static final BlockData ICE = Material.ICE.createBlockData();

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.FROST_NOVA;
	}

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"None can survive the Winter's frigid wrath.",
			"And yet, unspeakable things lie in the dark",
			"recesses of this frozen hellscape.");
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public Material getDisplayItem() {
		return Material.LIGHT_BLUE_GLAZED_TERRACOTTA;
	}

	@Override
	public void onCast(Plugin plugin, Player player, World world, double size) {
		Vector dir = player.getEyeLocation().getDirection();

		Vector crossXZ = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
		Vector crossY = dir.clone().crossProduct(crossXZ).normalize();

		new PPParametric(Particle.CRIT_MAGIC, player.getLocation().clone().add(dir.clone().multiply(0.7)).add(0, 1.5, 0), (parameter, builder) -> {
			double angle = parameter * Math.PI * 2;

			Vector launchDir = crossXZ.clone().multiply(FastUtils.cos(angle)).add(crossY.clone().multiply(FastUtils.sin(angle))).multiply(1.2);

			builder.offset(launchDir.getX(), launchDir.getY(), launchDir.getZ());
		}).count(50).directionalMode(true).extra(1.5).spawnAsPlayerActive(player);

		Bukkit.getScheduler().runTaskLater(plugin, () -> new PPParametric(Particle.CRIT_MAGIC, player.getLocation().clone().add(dir.clone().multiply(0.8)).add(0, 1.5, 0), (parameter, builder) -> {
			double angle = parameter * Math.PI * 2;

			Vector launchDir = crossXZ.clone().multiply(FastUtils.cos(angle)).add(crossY.clone().multiply(FastUtils.sin(angle))).multiply(0.8);

			builder.offset(launchDir.getX(), launchDir.getY(), launchDir.getZ());
		}).count(40).directionalMode(true).extra(1.5).spawnAsPlayerActive(player), 2);

		Bukkit.getScheduler().runTaskLater(plugin, () -> new PPParametric(Particle.CRIT_MAGIC, player.getLocation().clone().add(dir.clone().multiply(0.9)).add(0, 1.5, 0), (parameter, builder) -> {
			double angle = parameter * Math.PI * 2;

			Vector launchDir = crossXZ.clone().multiply(FastUtils.cos(angle)).add(crossY.clone().multiply(FastUtils.sin(angle))).multiply(0.4);

			builder.offset(launchDir.getX(), launchDir.getY(), launchDir.getZ());
		}).count(30).directionalMode(true).extra(1.5).spawnAsPlayerActive(player), 3);

		Location cLoc = player.getLocation();
		cLoc.setDirection(player.getLocation().getDirection().setY(0).normalize());
		Vector vec;
		double degree = -270;
		for (int step = 0; step < 18; step++, degree += 20) {
			double radian1 = Math.toRadians(degree);
			vec = new Vector(FastUtils.cos(radian1) * 0.5, 0.125, FastUtils.sin(radian1) * 0.5);
			vec = VectorUtils.rotateXAxis(vec, cLoc.getPitch());
			vec = VectorUtils.rotateYAxis(vec, cLoc.getYaw());

			Location l = cLoc.clone().add(0, 0.1, 0).add(vec);
			//mLoc is the player's location, l is the location of the spike, pre offset
			Vector toPlayer = VectorUtils.rotateTargetDirection(l.toVector().subtract(cLoc.clone().toVector()), 0, -20);

			Vector spikeDir = VectorUtils.randomUnitVector().multiply(0.2).add(toPlayer).setY(0.15).normalize();
			Location finalLoc = l.clone().add(spikeDir.multiply(1.8));
			double length = 2 * FastUtils.randomDoubleInRange(0.8, 1.2);
			drawLineSlash(finalLoc, spikeDir, 0, length, 0.15, 5, (Location lineLoc, double middleProgress, double endProgress, boolean middle) ->
				new PartialParticle(Particle.REDSTONE, lineLoc, 1, 0, 0, 0, 0, new Particle.DustOptions(
					ParticleUtils.getTransition(BASE_COLOR, TIP_COLOR, endProgress), Math.max(2.2f - (float) (endProgress * 1.35), 0.85f)))
					.spawnAsPlayerActive(player));
			drawLineSlash(finalLoc, spikeDir, 0, length, 0.4, 5, (Location lineLoc, double middleProgress, double endProgress, boolean middle) ->
				new PartialParticle(Particle.BLOCK_CRACK, lineLoc, 1, 0, 0, 0, 0, ICE)
					.spawnAsPlayerActive(player));
		}

		ParticleUtils.drawParticleCircleExplosion(player, player.getLocation(), 0, 1, -player.getLocation().getYaw(), -player.getLocation().getPitch(), 125,
			0.7f, true, 0, 0.1, Particle.EXPLOSION_NORMAL);
		ParticleUtils.drawParticleCircleExplosion(player, player.getLocation().add(0, 0.25, 0), 0, 1, -player.getLocation().getYaw(), -player.getLocation().getPitch(), 125,
			0.85f, true, 0, 0.1, Particle.EXPLOSION_NORMAL);

		Location loc = player.getLocation().add(0, 1, 0);
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1f, 1f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 1f, 0.5f);
		world.playSound(loc, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.PLAYERS, 1.2f, 0.5f);
		world.playSound(loc, Sound.ENTITY_POLAR_BEAR_WARNING, SoundCategory.PLAYERS, 1.0f, 0.5f);
		world.playSound(loc, Sound.ENTITY_WARDEN_ROAR, SoundCategory.PLAYERS, 1.0f, 1.25f);
	}

	@Override
	public void enemyEffect(Plugin plugin, Player player, LivingEntity enemy) {
		Location loc = enemy.getLocation().clone().add(0, 0.2, 0);
		loc.setDirection(loc.getDirection().setY(0).normalize());
		new PartialParticle(Particle.BLOCK_CRACK, loc, 30, 0.05, 0.05, 0.05, 0, ICE).spawnAsPlayerActive(player);
		ParticleUtils.drawParticleCircleExplosion(player, loc, 0, 1, 0, 0,
			50, 0.7f, true, 0, 0, Particle.SNOWFLAKE);
		ParticleUtils.drawParticleCircleExplosion(player, loc, 0, 1, 0, 0,
			30, 0.6f, true, 0, 0, Particle.SNOWFLAKE);
		Bukkit.getScheduler().runTaskLater(plugin, () -> spawnTendril(loc, player), 2L * FastUtils.randomIntInRange(0, 3));
	}

	private void spawnTendril(Location loc, Player mPlayer) {
		Location to = loc.clone().add(0, 8, 0);

		new BukkitRunnable() {
			final Location mL = loc.clone();
			int mT = 0;

			final int DURATION = FastUtils.RANDOM.nextInt(7, 11);
			final int ITERATIONS = 3;

			final double mXMult = FastUtils.randomDoubleInRange(-0.7, 0.7);
			final double mZMult = FastUtils.randomDoubleInRange(-0.7, 0.7);
			double mJ = 0;

			@Override
			public void run() {
				mT++;

				for (int i = 0; i < ITERATIONS; i++) {
					mJ++;
					float size = 0.5f + (1.7f * (1f - (float) (mJ / (ITERATIONS * DURATION))));
					double offset = 0.1 * (1f - (mJ / (ITERATIONS * DURATION)));
					double transition = mJ / (ITERATIONS * DURATION);
					double pi = (Math.PI * 2) * (1f - (mJ / (ITERATIONS * DURATION)));


					Vector vec = new Vector(mXMult * FastUtils.cos(pi), 0,
						mZMult * FastUtils.sin(pi));
					Location tendrilLoc = mL.clone().add(vec);

					new PartialParticle(Particle.REDSTONE, tendrilLoc, 3, offset, offset, offset, 0, new Particle.DustOptions(
						ParticleUtils.getTransition(BASE_COLOR, TIP_COLOR, transition), size))

						.spawnAsPlayerActive(mPlayer);

					mL.add(0, 0.25, 0);
					if (mL.distance(to) < 0.4) {
						this.cancel();
						return;
					}
				}

				if (mT >= DURATION) {
					this.cancel();
				}
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
