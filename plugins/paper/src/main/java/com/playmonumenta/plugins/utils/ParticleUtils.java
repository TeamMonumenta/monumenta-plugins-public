package com.playmonumenta.plugins.utils;

import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;



public class ParticleUtils {
	public enum BoundingBoxEdge {
		X_YMIN_ZMIN,
		X_YMIN_ZMAX,
		X_YMAX_ZMIN,
		X_YMAX_ZMAX,
		Y_XMIN_ZMIN,
		Y_XMIN_ZMAX,
		Y_XMAX_ZMIN,
		Y_XMAX_ZMAX,
		Z_XMIN_YMIN,
		Z_XMIN_YMAX,
		Z_XMAX_YMIN,
		Z_XMAX_YMAX
	}

	// TODO use Consumer?
	@FunctionalInterface
	public interface SpawnParticleAction {
		/**
		 * Spawns a particle at the specified location
		 */
		void run(Location loc);
	}

	public static void explodingRingEffect(Plugin plugin, Location loc, double radius, double height, int ticks, Collection<Map.Entry<Double, SpawnParticleAction>> particles) {
		new BukkitRunnable() {
			double mCurrentRadius = 0;

			@Override
			public void run() {
				mCurrentRadius += radius / ticks;

				for (double theta = 0; theta <= 2 * Math.PI; theta = theta + Math.PI / (7 * radius)) {
					double x = mCurrentRadius * FastUtils.cos(theta);
					double y = (FastUtils.RANDOM.nextDouble() - 0.5) * height;
					double z = mCurrentRadius * FastUtils.sin(theta);
					loc.add(x, y, z);

					for (Map.Entry<Double, SpawnParticleAction> particle : particles) {
						if (FastUtils.RANDOM.nextDouble() < particle.getKey()) {
							particle.getValue().run(loc);
						}
					}

					loc.subtract(x, y, z);
				}
				if (mCurrentRadius >= radius) {
					this.cancel();
				}
			}

		}.runTaskTimer(plugin, 0, 1);
	}

	public static void explodingConeEffect(Plugin plugin, LivingEntity entity, float radius, Particle type1, double percent1, Particle type2, double percent2, double dotAngle) {
		explodingConeEffect(plugin, entity, entity.getEyeLocation().getDirection().setY(0).normalize(), radius, type1, percent1, type2, percent2, dotAngle);
	}

	public static void explodingConeEffect(Plugin plugin, LivingEntity entity, Vector dir, float radius, Particle type1, double percent1, Particle type2, double percent2, double dotAngle) {
		new BukkitRunnable() {
			double mCurrentRadius = Math.PI / 4;
			Location mLoc = entity.getLocation();
			World mWorld = mLoc.getWorld();
			Vector mDirection = dir.setY(0).normalize();

			@Override
			public void run() {
				mCurrentRadius = mCurrentRadius + 0.25 * Math.PI;
				for (double theta = 0; theta <= 2 * Math.PI; theta = theta + Math.PI / 64) {
					double x = mCurrentRadius * FastUtils.cos(theta);
					double y = 2 * Math.exp(-0.1 * mCurrentRadius) * FastUtils.sin(mCurrentRadius) + 0.5;
					double z = mCurrentRadius * FastUtils.sin(theta);
					mLoc.add(x, y, z);

					Vector toParticle = mLoc.toVector().subtract(entity.getLocation().toVector()).setY(0).normalize();

					if (mDirection.dot(toParticle) > dotAngle && FastUtils.RANDOM.nextDouble() < percent1) {
						mWorld.spawnParticle(type1, mLoc, 1);
					}

					mLoc.subtract(x, y, z);

					theta = theta + Math.PI / 64;

					x = mCurrentRadius * FastUtils.cos(theta);
					y = 2 * Math.exp(-0.1 * mCurrentRadius) * FastUtils.sin(mCurrentRadius) + 1.5;
					z = mCurrentRadius * FastUtils.sin(theta);
					mLoc.add(x, y, z);

					toParticle = mLoc.toVector().subtract(entity.getLocation().toVector()).setY(0).normalize();

					if (mDirection.dot(toParticle) > dotAngle && FastUtils.RANDOM.nextDouble() < percent2) {
						mWorld.spawnParticle(type2, mLoc, 1);
					}

					mLoc.subtract(x, y, z);
				}
				if (mCurrentRadius > radius) {
					this.cancel();
				}
			}

		}.runTaskTimer(plugin, 0, 1);
	}

	public static void tickBoundingBoxEdge(World world, BoundingBox bb, Color color, int count) {
		Particle.DustOptions dustOptions = new Particle.DustOptions(color, 0.2f);
		Vector bbSize = bb.getMax().clone().subtract(bb.getMin());
		NavigableMap<Double, BoundingBoxEdge> edgeWeights = new TreeMap<>();
		double largestKey = 0.0;
		for (BoundingBoxEdge edge : BoundingBoxEdge.values()) {
			double edgeSize;
			switch (edge) {
			case X_YMIN_ZMIN:
			case X_YMIN_ZMAX:
			case X_YMAX_ZMIN:
			case X_YMAX_ZMAX:
				edgeSize = bbSize.getX();
				break;
			case Y_XMIN_ZMIN:
			case Y_XMIN_ZMAX:
			case Y_XMAX_ZMIN:
			case Y_XMAX_ZMAX:
				edgeSize = bbSize.getY();
				break;
			default:
				edgeSize = bbSize.getZ();
			}
			// Ensure bounding boxes with a size of 0 still show up
			edgeSize += 0.001;
			largestKey += edgeSize;
			edgeWeights.put(largestKey, edge);
		}
		for (int i = 0; i < count; i++) {
			Map.Entry<Double, BoundingBoxEdge> edgeEntry = edgeWeights.higherEntry(largestKey * FastUtils.RANDOM.nextDouble());
			if (edgeEntry == null) {
				// The reviewdog says this is a thing? Why is this a thing?
				break;
			}
			BoundingBoxEdge edge = edgeEntry.getValue();

			double x;
			switch (edge) {
			case X_YMIN_ZMIN:
			case X_YMIN_ZMAX:
			case X_YMAX_ZMIN:
			case X_YMAX_ZMAX:
				x = bb.getMinX() + bbSize.getX() * FastUtils.RANDOM.nextDouble();
				break;
			case Y_XMIN_ZMIN:
			case Y_XMIN_ZMAX:
			case Z_XMIN_YMIN:
			case Z_XMIN_YMAX:
				x = bb.getMinX();
				break;
			default:
				x = bb.getMaxX();
			}

			double y;
			switch (edge) {
			case Y_XMIN_ZMIN:
			case Y_XMIN_ZMAX:
			case Y_XMAX_ZMIN:
			case Y_XMAX_ZMAX:
				y = bb.getMinY() + bbSize.getY() * FastUtils.RANDOM.nextDouble();
				break;
			case X_YMIN_ZMIN:
			case X_YMIN_ZMAX:
			case Z_XMIN_YMIN:
			case Z_XMAX_YMIN:
				y = bb.getMinY();
				break;
			default:
				y = bb.getMaxY();
			}

			double z;
			switch (edge) {
			case Z_XMIN_YMIN:
			case Z_XMIN_YMAX:
			case Z_XMAX_YMIN:
			case Z_XMAX_YMAX:
				z = bb.getMinZ() + bbSize.getZ() * FastUtils.RANDOM.nextDouble();
				break;
			case X_YMIN_ZMIN:
			case X_YMAX_ZMIN:
			case Y_XMIN_ZMIN:
			case Y_XMAX_ZMIN:
				z = bb.getMinZ();
				break;
			default:
				z = bb.getMaxZ();
			}

			world.spawnParticle(Particle.REDSTONE, x, y, z, 1, 0.0, 0.0, 0.0, dustOptions);
		}
	}
}
