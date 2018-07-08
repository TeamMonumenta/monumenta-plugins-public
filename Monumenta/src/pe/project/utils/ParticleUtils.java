package pe.project.utils;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import pe.project.Plugin;
import pe.project.utils.particlelib.ParticleEffect;
import pe.project.utils.particlelib.ParticleEffect.OrdinaryColor;

public class ParticleUtils {
	public static void playParticleInWorld(World world, Particle type, Location loc, int count) {
		world.spawnParticle(type, loc, count);
	}

	public static void playParticlesInWorld(World world, Particle type, Location loc, int count, double xOffset, double yOffset, double zOffset, double data) {
		world.spawnParticle(type, loc, count, xOffset, yOffset, zOffset, data);
	}

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
						playParticleInWorld(world, type1, loc, 1);
					}

					loc.subtract(x, y, z);

					theta = theta + Math.PI / 64;

					x = t * Math.cos(theta);
					y = 2 * Math.exp(-0.1 * t) * Math.sin(t) + 1.5;
					z = t * Math.sin(theta);
					loc.add(x, y, z);

					if (rand.nextDouble() < percent2) {
						playParticleInWorld(world, type2, loc, 1);
					}

					loc.subtract(x, y, z);
				}
				if (t > radius) {
					this.cancel();
				}
			}

		}.runTaskTimer(plugin, 0, 1);
	}

	/**
	 * Plays Colored particle effects.
	 *
	 * Only works for REDSTONE, and SPELL_MOB
	 */
	public static void playColorEffect(ParticleEffect effect, int r, int g, int b, double xOffset, double yOffset, double zOffset, Location loc, int amount) {
		for (int i = 0; i < amount; i++) {
			double x = ThreadLocalRandom.current().nextDouble(-xOffset, xOffset);
			double y = ThreadLocalRandom.current().nextDouble(-yOffset, yOffset);
			double z = ThreadLocalRandom.current().nextDouble(-zOffset, zOffset);
			Location l = loc.clone();
			l.add(x, y, z);
			effect.display(new OrdinaryColor(r, g, b), l, 40);
		}
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
						playParticleInWorld(world, type1, loc, 1);
					}

					loc.subtract(x, y, z);

					theta = theta + Math.PI / 64;

					x = t * Math.cos(theta);
					y = 2 * Math.exp(-0.1 * t) * Math.sin(t) + 1.5;
					z = t * Math.sin(theta);
					loc.add(x, y, z);

					toParticle = loc.toVector().subtract(player.getLocation().toVector()).setY(0).normalize();

					if (playerDir.dot(toParticle) > dotAngle && rand.nextDouble() < percent2) {
						playParticleInWorld(world, type2, loc, 1);
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
