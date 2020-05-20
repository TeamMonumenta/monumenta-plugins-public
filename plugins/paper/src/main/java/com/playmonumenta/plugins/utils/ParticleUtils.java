package com.playmonumenta.plugins.utils;

import java.util.Collection;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ParticleUtils {

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

			public void run() {
				mCurrentRadius += radius / ticks;

				for (double theta = 0; theta <= 2 * Math.PI; theta = theta + Math.PI / (7 * radius)) {
					double x = mCurrentRadius * Math.cos(theta);
					double y = (FastUtils.RANDOM.nextDouble() - 0.5) * height;
					double z = mCurrentRadius * Math.sin(theta);
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
		new BukkitRunnable() {
			double mCurrentRadius = Math.PI / 4;
			Location mLoc = entity.getLocation();
			World mWorld = mLoc.getWorld();
			Vector mDirection = entity.getEyeLocation().getDirection().setY(0).normalize();

			public void run() {
				mCurrentRadius = mCurrentRadius + 0.25 * Math.PI;
				for (double theta = 0; theta <= 2 * Math.PI; theta = theta + Math.PI / 64) {
					double x = mCurrentRadius * Math.cos(theta);
					double y = 2 * Math.exp(-0.1 * mCurrentRadius) * Math.sin(mCurrentRadius) + 0.5;
					double z = mCurrentRadius * Math.sin(theta);
					mLoc.add(x, y, z);

					Vector toParticle = mLoc.toVector().subtract(entity.getLocation().toVector()).setY(0).normalize();

					if (mDirection.dot(toParticle) > dotAngle && FastUtils.RANDOM.nextDouble() < percent1) {
						mWorld.spawnParticle(type1, mLoc, 1);
					}

					mLoc.subtract(x, y, z);

					theta = theta + Math.PI / 64;

					x = mCurrentRadius * Math.cos(theta);
					y = 2 * Math.exp(-0.1 * mCurrentRadius) * Math.sin(mCurrentRadius) + 1.5;
					z = mCurrentRadius * Math.sin(theta);
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
}
