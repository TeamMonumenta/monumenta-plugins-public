package com.playmonumenta.plugins.cosmetics.skills.scout.hunter;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.particle.PPParametric;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class LockdownCS implements CosmeticSkill {

	private final Color START = Color.fromRGB(255, 1, 1);
	private final Color END = Color.fromRGB(1, 1, 1);

	private final Color[] COLORS = {START, END};

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.LOCKDOWN;
	}

	@Override
	public Material getDisplayItem() {
		return Material.BLAZE_POWDER;
	}

	public void lockdownShoot(Plugin plugin, Player player, Location loc) {
		World world = player.getWorld();
		new PartialParticle(Particle.CRIT, loc.clone().add(player.getLocation().getDirection()), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT_MAGIC, loc.clone().add(player.getLocation().getDirection()), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(player);

		world.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1f, 2f);
		world.playSound(player.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_OPEN_SHUTTER, SoundCategory.PLAYERS, 1f, 0.8f);
		world.playSound(player.getLocation(), Sound.ENTITY_BREEZE_DEATH, SoundCategory.PLAYERS, 0.7f, 1.2f);
		world.playSound(player.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, SoundCategory.PLAYERS, 1f, 0.4f);

		Location circleLoc = player.getLocation().add(0, 0.15, 0);
		circleLoc.setPitch(0);
		ParticleUtils.drawParticleCircleExplosion(player, circleLoc, 0, 2, 1, 0, 45, 0.6f, false, 0, Particle.SMOKE_NORMAL);

		Vector playerDir = player.getLocation().getDirection();

		Location front = loc.clone().add(playerDir.clone().multiply(2));

		Vector axis1 = playerDir.clone().crossProduct(new Vector(0, 1, 0)).normalize().multiply(0.3);
		Vector axis2 = playerDir.clone().crossProduct(axis1).normalize().multiply(0.3);

		for (int i = 0; i < 4; i++) {
			double start = i * (Math.PI / 2);

			// Particle Ring
			int x = (i == 0 || i == 3) ? 1 : -1;
			int y = (i < 2) ? 1 : -1;

			Vector cornerDir = axis1.clone().multiply(x)
				.add(axis2.clone().multiply(y))
				.normalize()
				.multiply(0.75);

			new PPParametric(Particle.REDSTONE, front, (parameter, builder) -> {
				double theta = start + parameter * (Math.PI / 2);

				Color color = ParticleUtils.getTransition(END, START, FastUtils.sin(Math.PI * parameter));

				builder.data(new Particle.DustOptions(color, 0.4f));
				builder.location(front.clone()
					.add(axis1.clone().multiply(FastUtils.cos(theta) * 3))
					.add(axis2.clone().multiply(FastUtils.sin(theta) * 3))
					.add(cornerDir)
					.add(VectorUtils.randomUnitVector().multiply(0.075)));
			})
				.count(120)
				.spawnAsPlayerActive(player);

			// Particle Lines

			Vector dir = axis1.clone().multiply(FastUtils.cos(start))
				.add(axis2.clone().multiply(FastUtils.sin(start)));

			double outer = 12;
			double inner = 3;

			new PPParametric(Particle.REDSTONE, front, (parameter, builder) -> {
				double dist = outer - parameter * (outer - inner);
				Color color = ParticleUtils.getTransition(END, START, FastUtils.sin(Math.PI * parameter));

				builder.data(new Particle.DustOptions(color, 0.8f));
				builder.location(front.clone().add(dir.clone().multiply(dist)));
			})
				.directionalMode(true)
				.count(25)
				.delta(0)
				.spawnAsPlayerActive(player);
		}
	}

	public void lockdownParticleLine(Player player, Location startLoc, Location endLoc, double radius, final double maxDistance) {
		double distance = maxDistance;
		double transitionDistance = maxDistance / (COLORS.length - 1);

		Location startingLoc = startLoc.clone().add(player.getLocation().getDirection().clone().multiply(2));

		Vector dir = LocationUtils.getDirectionTo(startingLoc, endLoc).multiply(-1); // What...

		for (int i = 0;
			 i < (COLORS.length - 1) && distance >= transitionDistance;
			 i++, distance -= transitionDistance) {
			final int I = i;

			Location startTransition = startingLoc.clone().add(dir.clone().multiply(I * transitionDistance));
			Location endTransition = startingLoc.clone().add(dir.clone().multiply((I + 1) * transitionDistance));

			ParticleUtils.drawLine(startTransition, endTransition, 80,
				(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1)
					.delta(radius)
					.data(new Particle.DustOptions(ParticleUtils.getTransition(COLORS[I], COLORS[I + 1], t / (double) 80), 1))
					.spawnAsPlayerActive(player));
		}
	}

	public void lockdownCharged(Player player) {
		World world = player.getWorld();

		world.playSound(player.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_DETECT_PLAYER, 1.1f, 1.7f);
		world.playSound(player.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, 1.1f, 1.8f);
		world.playSound(player.getLocation(), Sound.ITEM_SPYGLASS_USE, 1.5f, 0.4f);
	}

	public void lockdownTick(Player player, int count, int total, int t) {
		if (PremiumVanishIntegration.isInvisibleOrSpectator(player)) {
			return;
		}

		for (int i = 0; i < count; i++) {
			double angleOffset = 360 * (i / (double) total);

			double mVerticalAngle = 5.5 * t;
			double mRotationAngle = 10 * t + angleOffset;
			mVerticalAngle %= 360;
			mRotationAngle %= 360;

			Location particleLoc = LocationUtils
				.getHalfHeightLocation(player).add(
					FastUtils.cos(Math.toRadians(mRotationAngle)),
					FastUtils.sin(Math.toRadians(mVerticalAngle)) * 0.5,
					FastUtils.sin(Math.toRadians(mRotationAngle))
				);

			lockdownOrb(particleLoc, player);

		}
	}

	public void lockdownOrb(Location particleLoc, Player player) {
		new PartialParticle(Particle.DUST_COLOR_TRANSITION, particleLoc)
			.minimumCount(1)
			.distanceFalloff(16)
			.data(new Particle.DustTransition(START, END, 1.25f))
			.spawnAsPlayerActive(player);

		new PartialParticle(Particle.END_ROD, particleLoc)
			.minimumCount(1)
			.distanceFalloff(16)
			.extra(99999999)
			.spawnAsPlayerActive(player);
	}

	public void lockdownCharging(Player player, double progress) {
		player.getWorld().playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_MIDDLE, 0.35f, (float) (1.5f * progress));
	}

	public void lockdownHit(Player player, LivingEntity e) {
		e.getWorld().playSound(e.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1f, 0.8f);
	}

	public void lockdownMiss(Player player) {
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, SoundCategory.PLAYERS, 2f, 0.8f);
	}

	public void lockdownSuccess(Player player) {
		player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 1f, 0.4f);
		player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1f, 0.8f);
		player.playSound(player.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.PLAYERS, 0.4f, 2f);
	}
}
