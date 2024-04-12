package com.playmonumenta.plugins.cosmetics.skills.warlock.reaper;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import javax.annotation.Nullable;
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

public class StringofThornsCS extends VoodooBondsCS {
	private static final Color TRANSITION_RED_1 = Color.fromRGB(140, 0, 0);
	private static final Color TRANSITION_RED_2 = Color.fromRGB(200, 0, 0);
	private static final Particle.DustOptions RED_SMALL = new Particle.DustOptions(Color.fromRGB(200, 0, 0), 0.6f);
	private static final Particle.DustOptions DARK_GREEN = new Particle.DustOptions(Color.fromRGB(20, 80, 0), 1f);
	private static final Particle.DustOptions DARK_GREEN_SMALL = new Particle.DustOptions(Color.fromRGB(20, 80, 0), 0.8f);
	private boolean mTriggeredThisTick = false;

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Some believe in a red thread of fate.",
			"The answer may be much thornier."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.VINE;
	}

	@Override
	public @Nullable String getName() {
		return "String of Thorns";
	}

	@Override
	public void bondsStartEffect(World world, Player player, double radius) {
		Location loc = player.getLocation();
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 0.75f, 0.63f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1f, 2f);
		world.playSound(loc, Sound.ENTITY_CREEPER_DEATH, SoundCategory.PLAYERS, 1f, 0.7f);
		world.playSound(loc, Sound.ENTITY_GLOW_SQUID_AMBIENT, SoundCategory.PLAYERS, 1f, 0.5f);
		world.playSound(loc, Sound.ENTITY_GLOW_SQUID_AMBIENT, SoundCategory.PLAYERS, 1f, 0.6f);
		world.playSound(loc, Sound.ENTITY_GLOW_SQUID_AMBIENT, SoundCategory.PLAYERS, 1f, 0.75f);
		new BukkitRunnable() {
			final Location mLoc = LocationUtils.fallToGround(player.getLocation(), player.getLocation().getY() - 1);
			double mRadius = 1.75;
			double mRotation = 0;

			@Override
			public void run() {
				for (int i = 0; i < 12; i++) {
					new PPCircle(Particle.SMOKE_NORMAL, mLoc, mRadius + 0.75).extra(0.03).countPerMeter(0.15).spawnAsPlayerActive(player);

					Vector offset = VectorUtils.rotateYAxis(new Vector(mRadius, 0.075, 0), 90 + mRotation);
					Location pLoc = mLoc.clone().add(offset);

					Vector direction = LocationUtils.getDirectionTo(pLoc, mLoc);
					Location loc1 = pLoc.clone().add(VectorUtils.rotateYAxis(direction, 55).multiply(0.7));
					Location loc2 = pLoc.clone().add(VectorUtils.rotateYAxis(direction, -55).multiply(0.7));
					Location loc3 = pLoc.clone().add(direction.clone().multiply(0.7 * 2 * FastUtils.cosDeg(55)));

					new PPLine(Particle.REDSTONE, pLoc, loc1).data(DARK_GREEN)
						.countPerMeter(5).spawnAsPlayerActive(player);
					new PPLine(Particle.REDSTONE, pLoc, loc2).data(DARK_GREEN)
						.countPerMeter(5).spawnAsPlayerActive(player);
					new PPLine(Particle.REDSTONE, loc1, loc3).data(DARK_GREEN)
						.countPerMeter(5).spawnAsPlayerActive(player);
					new PPLine(Particle.REDSTONE, loc2, loc3).data(DARK_GREEN)
						.countPerMeter(5).spawnAsPlayerActive(player);

					mRadius += 0.03;
					mRotation += 360 * 2 / (1 + Math.sqrt(5)) % 360; // sunflower-esque golden ratio-ish

					if (mRadius >= radius - 0.5) {
						this.cancel();
					}
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
		new BukkitRunnable() {
			final Location mLoc = LocationUtils.fallToGround(player.getLocation(), player.getLocation().getY() - 1);
			double mRadius = 2.5;
			double mRotation = 0;

			@Override
			public void run() {

				for (int i = 0; i < 8; i++) {
					Vector offset = VectorUtils.rotateYAxis(new Vector(mRadius, 0.15, 0), 45 + mRotation);
					Location pLoc = mLoc.clone().add(offset);

					Vector direction = LocationUtils.getDirectionTo(pLoc, mLoc);
					Location loc1 = pLoc.clone().add(VectorUtils.rotateYAxis(direction, 33).multiply(0.5));
					Location loc2 = pLoc.clone().add(VectorUtils.rotateYAxis(direction, -33).multiply(0.5));
					Location loc3 = pLoc.clone().add(direction.clone().multiply(0.5 * 2 * FastUtils.cosDeg(33)));

					Particle.DustOptions color = new Particle.DustOptions(ParticleUtils.getTransition(TRANSITION_RED_1, TRANSITION_RED_2, mRadius / radius), 1f);
					new PPLine(Particle.REDSTONE, pLoc, loc1).data(color)
						.countPerMeter(5).spawnAsPlayerActive(player);
					new PPLine(Particle.REDSTONE, pLoc, loc2).data(color)
						.countPerMeter(5).spawnAsPlayerActive(player);
					new PPLine(Particle.REDSTONE, loc1, loc3).data(color)
						.countPerMeter(5).spawnAsPlayerActive(player);
					new PPLine(Particle.REDSTONE, loc2, loc3).data(color)
						.countPerMeter(5).spawnAsPlayerActive(player);

					mRadius += 0.045;
					mRotation -= 360 * 2 / (1 + Math.sqrt(5)) % 360; // sunflower-esque golden ratio-ish

					if (mRadius >= radius - 1) {
						this.cancel();
					}
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 2, 1);
	}

	@Override
	public void bondsApplyEffect(Player reaper, Player target) {
		if (reaper != target) {
			new PPLine(Particle.REDSTONE, LocationUtils.getHalfHeightLocation(reaper), LocationUtils.getHalfHeightLocation(target)).data(DARK_GREEN)
				.countPerMeter(18).delta(0.04).groupingDistance(0).spawnAsPlayerActive(reaper);
		}

		double red = RED_SMALL.getColor().getRed() / 255.0;
		double green = RED_SMALL.getColor().getGreen() / 255.0;
		double blue = RED_SMALL.getColor().getBlue() / 255.0;
		new PPCircle(Particle.SPELL_MOB, target.getLocation(), 0.5)
			.directionalMode(true).delta(red, green, blue).extra(1)
			.count(30).spawnAsPlayerActive(reaper);

		new PartialParticle(Particle.VILLAGER_HAPPY, LocationUtils.getHalfHeightLocation(target), 15, 0.5, 0.5, 0.5).spawnAsPlayerActive(reaper);
	}

	@Override
	public void bondsSpreadParticle(Player player, LivingEntity toMob, LivingEntity sourceMob) {
		World world = player.getWorld();
		Location sourceMobLoc = LocationUtils.getHalfHeightLocation(sourceMob);
		Location toMobLoc = LocationUtils.getHalfHeightLocation(toMob);

		if (!mTriggeredThisTick) {
			mTriggeredThisTick = true;
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> mTriggeredThisTick = false);
			world.playSound(sourceMobLoc, Sound.BLOCK_SWEET_BERRY_BUSH_PLACE, SoundCategory.PLAYERS, 0.8f, 0.9f);
			world.playSound(sourceMobLoc, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 0.8f, 0.85f);
			world.playSound(sourceMobLoc, Sound.BLOCK_CHAIN_STEP, SoundCategory.PLAYERS, 0.6f, 0.66f);
		}

		new PPLine(Particle.REDSTONE, sourceMobLoc, toMobLoc).data(DARK_GREEN_SMALL)
			.countPerMeter(16).groupingDistance(0).delta(0.03).spawnAsPlayerActive(player);

		// draw thorns along the way
		if (sourceMobLoc.distance(toMobLoc) > 1) {
			Vector direction = LocationUtils.getDirectionTo(toMobLoc, sourceMobLoc);
			Location thornLoc = sourceMobLoc.clone().add(direction);
			double length = 0.75;
			for (int i = 0; i < 8; i++) {
				double[] rotation = VectorUtils.vectorToRotation(direction);
				Vector offset = VectorUtils.rotateTargetDirection(
					new Vector(0.3 * (i % 2 == 0 ? 1 : -1), FastUtils.RANDOM.nextBoolean() ? 0.15 : -0.15, 0), rotation[0], rotation[1]);

				Location loc1 = thornLoc.clone();
				Location loc2 = thornLoc.clone().add(direction.clone().multiply(0.3));
				Location loc3 = thornLoc.clone().add(direction.clone().multiply(0.15)).add(offset);

				new PPLine(Particle.REDSTONE, loc1, loc3).data(RED_SMALL)
					.countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, loc2, loc3).data(RED_SMALL)
					.countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);

				thornLoc.add(direction.clone().multiply(length));

				if (thornLoc.distance(toMobLoc) < 0.75) {
					break;
				}
			}
		}
	}
}
