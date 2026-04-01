package com.playmonumenta.plugins.cosmetics.skills.scout.hunter;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
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

public class PredatorStrikeCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.PREDATOR_STRIKE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SPECTRAL_ARROW;
	}

	public void strikeTick(Player player, int tick) {
		new PartialParticle(Particle.SMOKE_NORMAL, player.getLocation().add(0, 0.75, 0), 1, 0.25, 0, 0.25, 0).spawnAsPlayerActive(player);
	}

	public void strikeParticleLine(Player player, Location startLoc, Location endLoc) {
		Vector dir = LocationUtils.getDirectionTo(endLoc, startLoc);

		new PPLine(Particle.SMOKE_NORMAL, startLoc, endLoc)
			.countPerMeter(20)
			.delta(0.15)
			.extra(0.075)
			.spawnAsPlayerActive(player);

		new PPLine(Particle.FLAME, startLoc, endLoc)
			.countPerMeter(4)
			.delta(0.2)
			.extra(0.1)
			.spawnAsPlayerActive(player);

		ParticleUtils.drawLine(startLoc, endLoc, 55,
			(l, t) -> {
				new PartialParticle(Particle.FLAME, l.clone().add(VectorUtils.randomUnitVector().multiply(1.25)))
					.count(1)
					.directionalMode(true)
					.delta(dir.getX(), dir.getY(), dir.getZ())
					.extra(0.4)
					.spawnAsPlayerActive(player);

				new PartialParticle(Particle.SMOKE_NORMAL, l.clone().add(VectorUtils.randomUnitVector().multiply(1.25)))
					.count(2)
					.directionalMode(true)
					.delta(dir.getX(), dir.getY(), dir.getZ())
					.extra(0.4)
					.spawnAsPlayerActive(player);
			});

		double[] rot = VectorUtils.vectorToRotation(player.getLocation().getDirection().multiply(-1));
		Location loc = startLoc.clone().add(player.getLocation().getDirection().multiply(2));

		for (int i = 0; i < 60; i++) {
			Vector newDir = VectorUtils.rotationToVector(rot[0] + FastUtils.randomDoubleInRange(-30, 30), rot[1] + FastUtils.randomDoubleInRange(-30, 30));
			Location newLoc = loc.clone().add(VectorUtils.randomUnitVector().multiply(1.25));

			new PartialParticle(Particle.SMOKE_NORMAL, newLoc, 2, newDir.getX(), FastUtils.randomDoubleInRange(-0.5, 0.5), newDir.getZ())
				.directionalMode(true)
				.extra(0.4)
				.spawnAsPlayerActive(player);

			new PartialParticle(Particle.FLAME, newLoc, 1, newDir.getX(), FastUtils.randomDoubleInRange(-0.5, 0.5), newDir.getZ())
				.directionalMode(true)
				.extra(0.4)
				.spawnAsPlayerActive(player);
		}

	}

	public void strikeSoundReady(World world, Player player) {
		world.playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_MIDDLE, SoundCategory.PLAYERS, 1.0f, 0.6f);
		world.playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_MIDDLE, SoundCategory.PLAYERS, 1.0f, 0.8f);
		world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 0.8f, 0.8f);
		world.playSound(player.getLocation(), Sound.ENTITY_CREEPER_HURT, SoundCategory.PLAYERS, 0.6f, 0.1f);
	}

	public void strikeLaunch(World world, Player player) {
		Location loc = player.getLocation();
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 2.0f, 1.2f);
		world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 2.0f, 0.6f);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 2.0f, 0.6f);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.PLAYERS, 2.0f, 0.6f);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 2.0f, 0.6f);
		world.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 0.7f, 0.1f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, 2.0f, 0.6f);
	}

	public void strikeSplinter(Player player, Location impactLoc, double angle, double radius) {
		World world = player.getWorld();

		strikeParticleLine(player, player.getEyeLocation(), impactLoc);

		world.playSound(impactLoc, Sound.ITEM_TRIDENT_RIPTIDE_1, 2f, 0.7f);
		world.playSound(impactLoc, Sound.ENTITY_ELDER_GUARDIAN_HURT, 2f, 0.4f);
		world.playSound(impactLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 0.6f);
		world.playSound(impactLoc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, 1.0f, 0.8f);
		world.playSound(impactLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 2.0f, 0.6f);
		world.playSound(impactLoc, Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 1f, 0.8f);
		world.playSound(impactLoc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 2.0f, 0.4f);
		world.playSound(impactLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 2.0f, 0.4f);
		world.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.2f, 0.4f);

		new PartialParticle(Particle.FLASH, impactLoc, 1, 0, 0, 0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.EXPLOSION_LARGE, impactLoc, 1, 0, 0, 0).spawnAsPlayerActive(player);

		final Location loc = player.getEyeLocation().subtract(0, 0.2, 0);
		loc.setDirection(player.getLocation().getDirection().normalize());

		new BukkitRunnable() {
			double mRadius = 0;

			@Override
			public void run() {
				if (mRadius > radius) {
					this.cancel();
					return;
				}

				Vector vec;
				mRadius += 1.25;
				double degree = 90 - angle;
				// particles about every 10 degrees
				int degreeSteps = (int) (angle / 5 * (1 + mRadius / radius));
				double degreeStep = 2 * angle / degreeSteps;

				for (int step = 0; step < degreeSteps; step++, degree += degreeStep) {
					double radian1 = Math.toRadians(degree);
					vec = new Vector(FastUtils.cos(radian1) * mRadius, 0.125, FastUtils.sin(radian1) * mRadius);
					vec = VectorUtils.rotateXAxis(vec, loc.getPitch());
					vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

					Location l = loc.clone().add(0, 0.1, 0).add(vec);

					new PartialParticle(Particle.FLAME, l)
						.count(3)
						.delta(0.2)
						.extra(0.05)
						.spawnAsPlayerActive(player);

					new PartialParticle(Particle.DUST_COLOR_TRANSITION, l)
						.count(2)
						.delta(0.2)
						.data(new Particle.DustTransition(Color.GRAY, Color.BLACK, 2))
						.spawnAsPlayerActive(player);

					new PartialParticle(Particle.SMOKE_NORMAL, l)
						.count(3)
						.delta(0.2)
						.extra(0.05)
						.spawnAsPlayerActive(player);
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

		double[] rot = VectorUtils.vectorToRotation(player.getLocation().getDirection());

		final double extra = radius / 12;

		for (int i = 0; i < angle; i++) {
			Vector newDir = VectorUtils.rotationToVector(rot[0] + FastUtils.randomDoubleInRange(-angle, angle), rot[1] + FastUtils.randomDoubleInRange(-angle / 2, angle / 2));

			new PartialParticle(Particle.SMALL_FLAME, loc, 2, newDir.getX(), FastUtils.randomDoubleInRange(-0.5, 0.5), newDir.getZ())
				.directionalMode(true)
				.extraRange(extra - 0.4, extra + 0.2)
				.spawnAsPlayerActive(player);

			new PartialParticle(Particle.SMOKE_NORMAL, loc, 3, newDir.getX(), FastUtils.randomDoubleInRange(-0.5, 0.5), newDir.getZ())
				.directionalMode(true)
				.extraRange(extra - 0.4, extra + 0.2)
				.spawnAsPlayerActive(player);

			new PartialParticle(Particle.SMOKE_LARGE, loc, 2, newDir.getX(), FastUtils.randomDoubleInRange(-0.5, 0.5), newDir.getZ())
				.directionalMode(true)
				.extraRange(extra - 0.4, extra + 0.2)
				.spawnAsPlayerActive(player);

			new PartialParticle(Particle.FLAME, loc, 2, newDir.getX(), FastUtils.randomDoubleInRange(-0.5, 0.5), newDir.getZ())
				.directionalMode(true)
				.extraRange(extra - 0.4, extra + 0.2)
				.spawnAsPlayerActive(player);
		}
	}

	public void strikeExplode(World world, Player player, Location loc, double radius) {
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1, 0.6f);
		world.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1, 0.6f);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 45, radius, radius, radius, 0.125).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FLAME, loc, 12, radius, radius, radius, 0.1).spawnAsPlayerActive(player);
	}

	public void strikeImpact(Runnable runnable, Location l, Player player) {
		runnable.run();
	}

	public void onUnprime(Player player, Location loc) {
		player.playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.0f, 0.5f);
	}
}
