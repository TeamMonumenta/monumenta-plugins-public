package com.playmonumenta.plugins.utils;

import java.util.Collection;
import java.util.Map;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ParticleUtils {
	private static final Random PARTICLE_RAND = new Random();

	@FunctionalInterface
	public interface SpawnParticleAction {
		/**
		 * Spawns a particle at the specified location
		 */
		void run(Location loc);
	}

	public static void explodingSphereEffect(Plugin plugin, Location loc, float radius, Collection<Map.Entry<Double, SpawnParticleAction>> particles) {
		new BukkitRunnable() {
			double t = Math.PI / 4;

			public void run() {
				t = t + 0.5 * Math.PI;
				for (double theta = 0; theta <= 2 * Math.PI; theta = theta + Math.PI / 32) {
					for (Map.Entry<Double, SpawnParticleAction> particle : particles) {
						theta = theta + Math.PI / 64;
						double x = t * Math.cos(theta);
						double y = 2 * Math.exp(-0.1 * t) * Math.sin(t) + 1.5;
						double z = t * Math.sin(theta);
						loc.add(x, y, z);

						if (PARTICLE_RAND.nextDouble() < particle.getKey()) {
							particle.getValue().run(loc);
						}

						loc.subtract(x, y, z);
					}
				}
				if (t > radius) {
					this.cancel();
				}
			}

		}.runTaskTimer(plugin, 0, 1);
	}

	public static void explodingConeEffect(Plugin plugin, LivingEntity entity, float radius, Particle type1, double percent1, Particle type2, double percent2, double dotAngle) {
		new BukkitRunnable() {
			double t = Math.PI / 4;
			Location loc = entity.getLocation();
			World world = loc.getWorld();
			Vector direction = entity.getEyeLocation().getDirection().setY(0).normalize();

			public void run() {
				t = t + 0.25 * Math.PI;
				for (double theta = 0; theta <= 2 * Math.PI; theta = theta + Math.PI / 64) {
					double x = t * Math.cos(theta);
					double y = 2 * Math.exp(-0.1 * t) * Math.sin(t) + 0.5;
					double z = t * Math.sin(theta);
					loc.add(x, y, z);

					Vector toParticle = loc.toVector().subtract(entity.getLocation().toVector()).setY(0).normalize();

					if (direction.dot(toParticle) > dotAngle && PARTICLE_RAND.nextDouble() < percent1) {
						world.spawnParticle(type1, loc, 1);
					}

					loc.subtract(x, y, z);

					theta = theta + Math.PI / 64;

					x = t * Math.cos(theta);
					y = 2 * Math.exp(-0.1 * t) * Math.sin(t) + 1.5;
					z = t * Math.sin(theta);
					loc.add(x, y, z);

					toParticle = loc.toVector().subtract(entity.getLocation().toVector()).setY(0).normalize();

					if (direction.dot(toParticle) > dotAngle && PARTICLE_RAND.nextDouble() < percent2) {
						world.spawnParticle(type2, loc, 1);
					}

					loc.subtract(x, y, z);
				}
				if (t > radius) {
					this.cancel();
				}
			}

		}.runTaskTimer(plugin, 0, 1);
	}
}
