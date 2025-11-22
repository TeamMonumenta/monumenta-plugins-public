package com.playmonumenta.plugins.cosmetics.skills.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class VorpalVinelashCS extends DaggerThrowCS {

	public static final String NAME = "Vorpal Vinelash";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Slice, strangle, sap.",
			"Nature takes life as much as it gives it."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.JUNGLE_SAPLING;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	private static final Color VINE_BASE = Color.fromRGB(8, 54, 23);
	private static final Color VINE_TIP = Color.fromRGB(119, 201, 56);

	@Override
	public void daggerThrowEffect(World world, Location loc, Player player) {
		Vector dir = player.getEyeLocation().getDirection();
		Vector up = VectorUtils.rotateTargetDirection(dir, 0, 90);
		Vector right = VectorUtils.crossProd(up, dir);

		new PPCircle(Particle.CRIT, loc, 0.1)
			.count(32).directionalMode(true).rotateDelta(true).delta(up.getX(), up.getY(), up.getZ())
			.axes(up, right).extra(2).spawnAsPlayerActive(player);


		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () ->
			world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 2.0f, 2f), 3);
		world.playSound(loc, Sound.ITEM_BONE_MEAL_USE, SoundCategory.PLAYERS, 0.9f, 0.5f);
		world.playSound(loc, Sound.BLOCK_CHORUS_FLOWER_GROW, SoundCategory.PLAYERS, 2f, 1.5f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1f, 0.7f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 0.7f, 0.7f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 0.7f, 1.5f);
	}

	@Override
	public void daggerParticle(Location startLoc, Location endLoc, Player player) {
		Location l = startLoc.clone().subtract(0, 0.3, 0);
		Vector dir = LocationUtils.getDirectionTo(startLoc, endLoc).multiply(-1.0 / 9);
		double distance = startLoc.distance(endLoc);
		new BukkitRunnable() {
			double mRotation = FastUtils.randomIntInRange(0, 360);
			double mRadius = 0;
			double mSize = 1.2;
			double mIter = 0;

			@Override
			public void run() {
				mIter++;
				if (mIter > 3) {
					this.cancel();
				}
				for (int i = 0; i < distance * mIter; i++) {
					l.add(dir);
					mRotation += 6;

					if (i < distance * 1.5) {
						mRadius += 0.75D / (distance * 9);
					} else {
						mSize -= 1.2D / (distance * 9);
						mRadius -= 0.75D / (distance * 9);
					}

					double radian = FastMath.toRadians(mRotation);
					Vector vec = new Vector(FastUtils.cos(radian) * mRadius, 0,
						FastUtils.sin(radian) * mRadius);
					vec = VectorUtils.rotateXAxis(vec, l.getPitch() + 90);
					vec = VectorUtils.rotateYAxis(vec, l.getYaw());
					Location helixLoc = l.clone().add(vec);

					new PartialParticle(Particle.REDSTONE, helixLoc, 2, 0, 0, 0, 0, new Particle.DustOptions(
						ParticleUtils.getTransition(VINE_TIP, VINE_BASE, (l.distance(endLoc) / distance)), (float) mSize)).spawnAsPlayerActive(player);
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void daggerHitEffect(World world, Location loc, LivingEntity target, Player player) {
		Vector dir = LocationUtils.getDirectionTo(LocationUtils.getHalfHeightLocation(target), loc);
		Vector up = VectorUtils.rotateTargetDirection(dir, 0, 90);
		Vector right = VectorUtils.crossProd(up, dir);
		new PPCircle(Particle.CRIT_MAGIC, target.getEyeLocation(), 0.1)
			.count(32).directionalMode(true).rotateDelta(true).axes(up, right).delta(up.getX(), up.getY(), up.getZ())
			.extra(1.2).spawnAsPlayerActive(player);
		new PPCircle(Particle.CRIT_MAGIC, target.getEyeLocation(), 0.1)
			.count(32).directionalMode(true).rotateDelta(true).axes(up, right).delta(up.getX(), up.getY(), up.getZ())
			.extra(1.7).spawnAsPlayerActive(player);
		new PPCircle(Particle.CRIT, target.getLocation(), 0.1)
			.count(32).directionalMode(true).rotateDelta(true).delta(0.8, 0.2, 0)
			.extra(1.5).spawnAsPlayerActive(player);
		new PPCircle(Particle.DUST_COLOR_TRANSITION, LocationUtils.getHalfHeightLocation(target).add(0, -0.33, 0),
			target.getWidth() * 1.25).data(new Particle.DustTransition(VINE_BASE, VINE_TIP, 1f))
			.countPerMeter(6).spawnAsPlayerActive(player);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () ->
			new PPCircle(Particle.DUST_COLOR_TRANSITION, LocationUtils.getHalfHeightLocation(target).add(0, 0.33, 0),
				target.getWidth() * 1.25).data(new Particle.DustTransition(VINE_BASE, VINE_TIP, 1f))
				.countPerMeter(6).spawnAsPlayerActive(player), 2);
		new PartialParticle(Particle.SPORE_BLOSSOM_AIR, LocationUtils.getHalfHeightLocation(target), 16).spawnAsPlayerActive(player);
		world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, SoundCategory.PLAYERS, 1f, 1f);
		world.playSound(target.getLocation(), Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1f, 0.5f);
	}

	@Override
	public void daggerHitBlockEffect(Location bLoc, Player player) {
		new PartialParticle(Particle.SPORE_BLOSSOM_AIR, bLoc, 16).spawnAsPlayerActive(player);
	}
}
