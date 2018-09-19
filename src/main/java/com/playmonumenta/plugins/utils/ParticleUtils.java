package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.Plugin;

import java.util.concurrent.ThreadLocalRandom;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.World;

public class ParticleUtils {
	public static void explodingSphereEffect(Plugin plugin, Player player, float radius, Particle type1, double percent1, Particle type2, double percent2) {
		new BukkitRunnable() {
			double t = Math.PI / 4;
			Location loc = player.getLocation();
			World world = Bukkit.getWorld(player.getWorld().getName());
			Random rand = new Random();
			public void run() {
				t = t + 0.5 * Math.PI;
				for (double theta = 0; theta <= 2 * Math.PI; theta = theta + Math.PI / 32) {
					double x = t * Math.cos(theta);
					double y = 2 * Math.exp(-0.1 * t) * Math.sin(t) + 1.5;
					double z = t * Math.sin(theta);
					loc.add(x, y, z);

					if (rand.nextDouble() < percent1) {
						world.spawnParticle(type1, loc, 1);
					}

					loc.subtract(x, y, z);

					theta = theta + Math.PI / 64;

					x = t * Math.cos(theta);
					y = 2 * Math.exp(-0.1 * t) * Math.sin(t) + 1.5;
					z = t * Math.sin(theta);
					loc.add(x, y, z);

					if (rand.nextDouble() < percent2) {
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

	public static void explodingConeEffect(Plugin plugin, Player player, float radius, Particle type1, double percent1, Particle type2, double percent2, double dotAngle) {
		new BukkitRunnable() {
			double t = Math.PI / 4;
			Location loc = player.getLocation();
			World world = Bukkit.getWorld(player.getWorld().getName());
			Random rand = new Random();
			Vector playerDir = player.getEyeLocation().getDirection().setY(0).normalize();

			public void run() {
				t = t + 0.25 * Math.PI;
				for (double theta = 0; theta <= 2 * Math.PI; theta = theta + Math.PI / 64) {
					double x = t * Math.cos(theta);
					double y = 2 * Math.exp(-0.1 * t) * Math.sin(t) + 0.5;
					double z = t * Math.sin(theta);
					loc.add(x, y, z);

					Vector toParticle = loc.toVector().subtract(player.getLocation().toVector()).setY(0).normalize();

					if (playerDir.dot(toParticle) > dotAngle && rand.nextDouble() < percent1) {
						world.spawnParticle(type1, loc, 1);
					}

					loc.subtract(x, y, z);

					theta = theta + Math.PI / 64;

					x = t * Math.cos(theta);
					y = 2 * Math.exp(-0.1 * t) * Math.sin(t) + 1.5;
					z = t * Math.sin(theta);
					loc.add(x, y, z);

					toParticle = loc.toVector().subtract(player.getLocation().toVector()).setY(0).normalize();

					if (playerDir.dot(toParticle) > dotAngle && rand.nextDouble() < percent2) {
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
