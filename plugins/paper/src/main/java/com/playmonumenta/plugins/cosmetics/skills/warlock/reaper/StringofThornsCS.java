package com.playmonumenta.plugins.cosmetics.skills.warlock.reaper;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import com.playmonumenta.scriptedquests.utils.MetadataUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class StringofThornsCS extends VoodooBondsCS {
	private static final Particle.DustOptions RED_SMALL = new Particle.DustOptions(Color.fromRGB(200, 0, 0), 0.6f);
	private static final Particle.DustOptions DARK_GREEN = new Particle.DustOptions(Color.fromRGB(0, 80, 20), 1f);
	private static final Particle.DustOptions DARK_GREEN_SMALL = new Particle.DustOptions(Color.fromRGB(0, 80, 20), 0.8f);
	private static final Particle.DustOptions TRANSITION_GREEN_1 = new Particle.DustOptions(Color.fromRGB(0, 85, 35), 1f);
	private static final Particle.DustOptions TRANSITION_GREEN_2 = new Particle.DustOptions(Color.fromRGB(110, 175, 0), 1f);

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
	public void launchPin(Player player, Location startLoc, Location endLoc) {
		World world = player.getWorld();
		world.playSound(startLoc, Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 0.8f, 1.7f);
		world.playSound(startLoc, Sound.ENTITY_CREEPER_DEATH, SoundCategory.PLAYERS, 1f, 0.7f);
		world.playSound(startLoc, Sound.BLOCK_GRASS_STEP, SoundCategory.PLAYERS, 1f, 0.6f);
		world.playSound(startLoc, Sound.BLOCK_VINE_STEP, SoundCategory.PLAYERS, 1f, 0.8f);
		world.playSound(startLoc, Sound.BLOCK_LANTERN_BREAK, SoundCategory.PLAYERS, 1f, 0.5f);

		Vector direction = LocationUtils.getDirectionTo(endLoc, startLoc);
		new BukkitRunnable() {
			int mTicks = 0;
			final Location mLoc = startLoc.clone();
			final Vector mDir = direction.clone();

			@Override
			public void run() {
				for (int i = 0; i < 8; i++) {
					Vector offset = VectorUtils.rotateTargetDirection(mDir, 90 * (i % 2 == 0 ? 1 : -1), 0).multiply(0.45);
					Location pLoc = mLoc.clone().add(offset);
					pLoc = LocationUtils.fallToGround(pLoc, pLoc.getY() - 4);
					pLoc.add(0, 0.1, 0);
					double angle = startLoc.getYaw() + 135 + 60 * (i % 2 == 0 ? 1 : -1);
					Location loc1 = pLoc.clone().add(VectorUtils.rotateYAxis(new Vector(0.2, 0, 0.2), angle));
					Location loc2 = pLoc.clone().add(VectorUtils.rotateYAxis(new Vector(-0.35, 0, 0.35), angle));
					Location loc3 = pLoc.clone().add(VectorUtils.rotateYAxis(new Vector(-0.2, 0, -0.2), angle));
					Location loc4 = pLoc.clone().add(VectorUtils.rotateYAxis(new Vector(0.55, 0, -0.55), angle));

					Particle.DustOptions color = ParticleUtils.getTransition(TRANSITION_GREEN_2, TRANSITION_GREEN_1, mLoc.distance(endLoc) / startLoc.distance(endLoc) * 1.1);
					new PPLine(Particle.REDSTONE, loc1, loc2).data(color)
						.countPerMeter(10).groupingDistance(0).spawnAsPlayerActive(player);
					new PPLine(Particle.REDSTONE, loc2, loc3).data(color)
						.countPerMeter(10).groupingDistance(0).spawnAsPlayerActive(player);
					new PPLine(Particle.REDSTONE, loc3, loc4).data(color)
						.countPerMeter(10).groupingDistance(0).spawnAsPlayerActive(player);
					new PPLine(Particle.REDSTONE, loc4, loc1).data(color)
						.countPerMeter(10).groupingDistance(0).spawnAsPlayerActive(player);
					new PartialParticle(Particle.BLOCK_CRACK, pLoc, 4, 0.1, 0.1, 0.1).data(Material.JUNGLE_LEAVES.createBlockData()).spawnAsPlayerActive(player);


					mLoc.add(mDir.clone().multiply(0.5));
					if (mLoc.distance(endLoc) < 0.55) {
						this.cancel();
					}
				}

				mTicks++;
				if (mTicks >= 10) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

		startLoc.subtract(0, 2, 0);
		new BukkitRunnable() {
			double mD = 30;

			@Override
			public void run() {
				Vector vec;
				for (double degree = mD; degree < mD + 40; degree += 8) {
					double radian1 = Math.toRadians(degree);
					double cos = FastUtils.cos(radian1);
					double sin = FastUtils.sin(radian1);
					for (double r = 1; r < 5; r += 0.5) {
						vec = new Vector(cos * r, 1, sin * r);
						vec = VectorUtils.rotateXAxis(vec, startLoc.getPitch());
						vec = VectorUtils.rotateYAxis(vec, startLoc.getYaw());

						Location l = startLoc.clone().add(vec);
						DustOptions color = ParticleUtils.getTransition(TRANSITION_GREEN_1, TRANSITION_GREEN_2, l.distance(endLoc) / startLoc.distance(endLoc) * 1.1);
						new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, color).spawnAsPlayerActive(player);
					}
				}
				mD += 40;
				if (mD >= 150) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void hitMob(Player player, LivingEntity mob) {
		World world = player.getWorld();
		Location loc = LocationUtils.getEntityCenter(mob);

		world.playSound(loc, Sound.BLOCK_SWEET_BERRY_BUSH_PLACE, SoundCategory.PLAYERS, 1.0f, 0.9f);
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 1.0f, 0.85f);
		world.playSound(loc, Sound.BLOCK_CHAIN_STEP, SoundCategory.PLAYERS, 1.0f, 0.66f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 0.6f, 1.5f);

		new PartialParticle(Particle.GLOW_SQUID_INK, loc, 8, 0.25, 0.5, 0.25, 0.05).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FALLING_DUST, loc, 25, 0.35, 0.6, 0.35, 0).data(Material.LIME_CONCRETE.createBlockData()).spawnAsPlayerActive(player);
	}

	@Override
	public void hitPlayer(Player reaper, Player target) {
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
	public void curseTick(Player player, Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (entity instanceof LivingEntity mob) {
			Location loc = mob.getEyeLocation();
			new PartialParticle(Particle.SNEEZE, loc, 5)
				.directionalMode(true).delta(0, 1, 0).extraRange(0.10, 0.20).spawnAsPlayerActive(player);
		}
	}

	@Override
	public void curseSpread(Player player, LivingEntity toMob, LivingEntity sourceMob) {
		World world = player.getWorld();
		Location sourceMobLoc = LocationUtils.getHalfHeightLocation(sourceMob);
		Location toMobLoc = LocationUtils.getHalfHeightLocation(toMob);

		if (MetadataUtils.checkOnceThisTick(Plugin.getInstance(), player, "StringofThornsCurseSound")) {
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
