package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class IngressCS extends ThunderStepCS {
	public static final String NAME = "Ingress";
	public static final Color COLOR_RED = Color.fromRGB(201, 0, 74);
	public static final Color COLOR_BLACK = Color.fromRGB(0, 0, 0);
	private static final Particle.DustOptions BLACK_DUST = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.5f);
	private static final Particle.DustOptions DARK_DUST_TRANSITION = new Particle.DustOptions(Color.fromRGB(54, 6, 28), 1.4f);
	private static final Particle.DustOptions RED_DUST_TRANSITION = new Particle.DustOptions(Color.fromRGB(92, 2, 43), 1.4f);
	private static final Particle.DustOptions RED_DUST = new Particle.DustOptions(Color.fromRGB(201, 0, 74), 1.3f);

	private @Nullable BukkitRunnable mRunnable = null;

	private static final double ANIM_RADIUS = 0.08;
	private static final double OFFSET = -1.27;
	private static final double Y_OFFSET = 1.15;

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"It is said that the pure power of",
			"dissonance pierces through the void",
			"itself... A way in, at long last...");
	}

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.THUNDER_STEP;
	}

	@Override
	public Material getDisplayItem() {
		return Material.ENDER_EYE;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public void castEffect(Player player, double ratio, double radius) {
		Location location = player.getLocation();
		World world = location.getWorld();
		world.playSound(location, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 1f, 1f);
		world.playSound(location, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1f, 1.65f);
		for (int i = 0; i < 5; i++) {
			sparkParticle(player, LocationUtils.getHalfHeightLocation(player), LocationUtils.getHalfHeightLocation(player), VectorUtils.rotateTargetDirection(location.getDirection().setY(0).normalize(), FastUtils.randomDoubleInRange(0, 360), FastUtils.randomIntInRange(-85, 20)), 0.75f, ratio * 4, 0);
		}
		new PartialParticle(Particle.FLASH, location.clone().add(location.getDirection()), 1, 0, 0, 0, 10)
			.minimumCount(1).spawnAsPlayerActive(player);
		new PPExplosion(Particle.SQUID_INK, player.getLocation().clone().add(0, 1.2, 0))
			.extra(0.5)
			.count(20)
			.spawnAsBoss();
	}

	@Override
	public void trailEffect(Player player, Location startLoc, Location endLoc) {
		new PPLine(Particle.DUST_COLOR_TRANSITION, startLoc.clone().add(0, 1.3, 0), endLoc.clone().add(0, 1, 0)).countPerMeter(5).data(new Particle.DustTransition(COLOR_BLACK, COLOR_RED, 0.8f))
			.countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
		new PPLine(Particle.ELECTRIC_SPARK, startLoc.clone().add(0, 1.3, 0), endLoc.clone().add(0, 1, 0)).countPerMeter(2).minParticlesPerMeter(0).delta(0).extra(99999).spawnAsPlayerActive(player);
	}

	//3 sets of a snippet of like half a period of 2 sin waves touching, put on the coordinate system of the vector along the shape itself as well as the vector orthogonal to it and the direction my player is looking
	@Override
	public void lingeringEffect(Plugin plugin, Player player, Location startLoc, int duration) {
		Vector firstCross = player.getLocation().clone().getDirection().setY(0).normalize().crossProduct(new Vector(0, 1, 0));
		Vector alongShape = firstCross.multiply(1).add(new Vector(0, 1, 0));
		Vector crossWithLook = player.getLocation().clone().getDirection().setY(0).normalize().getCrossProduct(alongShape);
		if (mRunnable == null || mRunnable.isCancelled()) {
			mRunnable = new BukkitRunnable() {
				int mTicks = 0;
				int mFrame = 0;

				@Override
				public void run() {
					if (mTicks == 0) {
						startLoc.getWorld().playSound(startLoc, Sound.AMBIENT_CRIMSON_FOREST_MOOD, SoundCategory.PLAYERS, 1f, 2f);
						startLoc.getWorld().playSound(startLoc, Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 1f, 1.15f);
					}

					if (mTicks % 2 == 0) {
						// black outline
						new PPParametric(Particle.REDSTONE, startLoc, (parameter, builder) -> {
							double x = -(ANIM_RADIUS * Math.min(mFrame, 3)) * FastUtils.sin(6.25 * parameter - 1.5) - (ANIM_RADIUS * Math.min(mFrame, 3)) + OFFSET;
							double y = 2.5 * parameter;
							Vector vec = alongShape.clone().multiply(y).add(crossWithLook.clone().multiply(x));
							builder.location(startLoc.clone().subtract(0, Y_OFFSET, 0).add(vec));
						}).data(BLACK_DUST).directionalMode(true).count(20).extra(0).spawnAsPlayerActive(player);

						new PPParametric(Particle.REDSTONE, startLoc.clone(), (parameter, builder) -> {
							double x = (ANIM_RADIUS * Math.min(mFrame, 3)) * FastUtils.sin(6.25 * parameter - 1.5) + (ANIM_RADIUS * Math.min(mFrame, 3)) + OFFSET;
							double y = 2.5 * parameter;
							Vector vec = alongShape.clone().multiply(y).add(crossWithLook.clone().multiply(x));
							builder.location(startLoc.clone().subtract(0, Y_OFFSET, 0).add(vec));
						}).data(BLACK_DUST).directionalMode(true).count(20).extra(0).spawnAsPlayerActive(player);

						if (mFrame > 0) {
							// dark red part, outer
							new PPParametric(Particle.REDSTONE, startLoc, (parameter, builder) -> {
								double x = -(ANIM_RADIUS * (Math.min(mFrame, 3) - 1)) * FastUtils.sin(6.25 * parameter - 1.5) - (ANIM_RADIUS * (Math.min(mFrame, 3) - 1)) + OFFSET;
								double y = 2.5 * parameter;
								Vector vec = alongShape.clone().multiply(y).add(crossWithLook.clone().multiply(x));
								builder.location(startLoc.clone().subtract(0, Y_OFFSET, 0).add(vec));
							}).data(DARK_DUST_TRANSITION).directionalMode(true).count(25).extra(0).spawnAsPlayerActive(player);

							new PPParametric(Particle.REDSTONE, startLoc.clone(), (parameter, builder) -> {
								double x = (ANIM_RADIUS * (Math.min(mFrame, 3) - 1)) * FastUtils.sin(6.25 * parameter - 1.5) + (ANIM_RADIUS * (Math.min(mFrame, 3) - 1)) + OFFSET;
								double y = 2.5 * parameter;
								Vector vec = alongShape.clone().multiply(y).add(crossWithLook.clone().multiply(x));
								builder.location(startLoc.clone().subtract(0, Y_OFFSET, 0).add(vec));
							}).data(DARK_DUST_TRANSITION).directionalMode(true).count(25).extra(0).spawnAsPlayerActive(player);

						}
						if (mFrame > 1) {
							// slightly lighter red part, middle
							new PPParametric(Particle.REDSTONE, startLoc, (parameter, builder) -> {
								double x = -(ANIM_RADIUS * (Math.min(mFrame, 3) - 2)) * FastUtils.sin(6.25 * parameter - 1.5) - (ANIM_RADIUS * (Math.min(mFrame, 3) - 2)) + OFFSET;
								double y = 2.5 * parameter;
								Vector vec = alongShape.clone().multiply(y).add(crossWithLook.clone().multiply(x));
								builder.location(startLoc.clone().subtract(0, Y_OFFSET, 0).add(vec));
							}).data(RED_DUST_TRANSITION).directionalMode(true).count(15).extra(0).spawnAsPlayerActive(player);

							new PPParametric(Particle.REDSTONE, startLoc.clone(), (parameter, builder) -> {
								double x = (ANIM_RADIUS * (Math.min(mFrame, 3) - 2)) * FastUtils.sin(6.25 * parameter - 1.5) + (ANIM_RADIUS * (Math.min(mFrame, 3) - 2)) + OFFSET;
								double y = 2.5 * parameter;
								Vector vec = alongShape.clone().multiply(y).add(crossWithLook.clone().multiply(x));
								builder.location(startLoc.clone().subtract(0, Y_OFFSET, 0).add(vec));
							}).data(RED_DUST_TRANSITION).directionalMode(true).count(15).extra(0).spawnAsPlayerActive(player);

							// red part, inner
							new PPParametric(Particle.REDSTONE, startLoc, (parameter, builder) -> {
								double y = 0.75 + parameter;
								Vector vec = alongShape.clone().multiply(y).add(crossWithLook.clone().multiply(OFFSET));
								builder.location(startLoc.clone().subtract(0, Y_OFFSET, 0).add(vec));
							}).data(RED_DUST).directionalMode(true).count(10).extra(0).spawnAsPlayerActive(player);
						}
						mFrame++;
					}

					new PartialParticle(Particle.REDSTONE, player.getLocation(), 2, 0.5, 0.1, 0.5, 0.1, RED_DUST).spawnAsPlayerActive(player);
					new PartialParticle(Particle.CRIMSON_SPORE, player.getLocation(), 1, 0.3, 0.1, 0.3, 0.005).spawnAsPlayerActive(player);
					new PartialParticle(Particle.CRIMSON_SPORE, startLoc.clone().add(0, 1.25, 0), 1, 0.3, 0.3, 0.3, 0.005).spawnAsPlayerActive(player);
					new PartialParticle(Particle.SQUID_INK, startLoc.clone().add(0, 1.25, 0), 1, 0, 0, 0, 0.25)
						.minimumCount(1).spawnAsPlayerActive(player);

					if (mTicks >= duration) {
						this.cancel();
						new PPExplosion(Particle.SQUID_INK, startLoc.clone().add(0, 1.2, 0))
							.extra(0.5)
							.count(20)
							.spawnAsPlayerActive(player);
						startLoc.getWorld().playSound(startLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 1f, 0.9f);
						player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1f, 0.75f);
						mTicks = 0;
						mRunnable = null;
					}
					mTicks++;
				}
			};
			mRunnable.runTaskTimer(plugin, 0, 1);
		}
	}

	@Override
	public void onDamage(Player player, LivingEntity enemy, int mobParticles) {
		Location enemyParticleLocation = enemy.getLocation().add(0, enemy.getHeight() / 2, 0);
		new PartialParticle(Particle.DAMAGE_INDICATOR, enemyParticleLocation, mobParticles, 0.5, 0.5, 0.5, 0.5).spawnAsPlayerActive(player);
		new PartialParticle(Particle.ELECTRIC_SPARK, enemyParticleLocation, mobParticles, 0.5, 0.5, 0.5, 0.5).spawnAsPlayerActive(player);
	}

	private void sparkParticle(Player player, Location loc, Location origin, Vector dir, float size, double radius, int iteration) {
		if (loc.distance(origin) > radius || iteration > 2) {
			return;
		}

		Location location = loc.clone();
		Vector direction = dir.clone();

		for (int i = 0; i < 2; i++) {
			Location oldLocation = location.clone();
			location.add(direction.multiply(0.7)).add(FastUtils.randomDoubleInRange(-0.25, 0.25), FastUtils.randomDoubleInRange(-0.25, 0.25), FastUtils.randomDoubleInRange(-0.25, 0.25));

			new PPLine(Particle.DUST_COLOR_TRANSITION, oldLocation, location).data(new Particle.DustTransition(COLOR_BLACK, COLOR_RED, size))
				.countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
		}
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			sparkParticle(player, location, origin, dir, size, radius, iteration + 1);
			sparkParticle(player, location, origin, dir, size, radius, iteration + 1);
		}, 1);
	}

	@Override
	public void playerTeleportedBack() {
		if (mRunnable != null) {
			mRunnable.cancel();
			mRunnable = null;
		}
	}
}
