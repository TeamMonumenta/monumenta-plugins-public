package com.playmonumenta.plugins.cosmetics.skills.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class FloralFlurryCS extends CursedWoundCS {
	private static final Particle.DustOptions NEON = new Particle.DustOptions(Color.fromRGB(215, 255, 0), 1f);
	private static final Particle.DustOptions NEON_SMALL = new Particle.DustOptions(Color.fromRGB(215, 255, 0), 0.6f);
	private static final Particle.DustOptions TEAL = new Particle.DustOptions(Color.fromRGB(0, 200, 140), 1f);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"A verdant insignia brands your strikes.",
			"It is unwise to be on the wrong side of nature."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.DANDELION;
	}

	@Override
	public @Nullable String getName() {
		return "Floral Flurry";
	}

	@Override
	public void onAttack(Player player, Entity entity) {
		new PartialParticle(Particle.TOTEM, LocationUtils.getHalfHeightLocation(entity), 5, 0.5, 0.5, 0.5, 0.3).spawnAsPlayerActive(player);
	}

	@Override
	public void onCriticalAttack(World world, Player player, LivingEntity mob, int cooldowns) {
		Location playerLoc = player.getLocation();
		world.playSound(playerLoc, Sound.ENTITY_PHANTOM_HURT, SoundCategory.PLAYERS, 0.5f, 1.5f);
		world.playSound(playerLoc, Sound.BLOCK_PISTON_CONTRACT, SoundCategory.PLAYERS, 0.4f, 1.5f);
		world.playSound(playerLoc, Sound.BLOCK_GRASS_BREAK, SoundCategory.PLAYERS, 0.6f, 0.65f);
		world.playSound(playerLoc, Sound.ENTITY_GUARDIAN_HURT, SoundCategory.PLAYERS, 0.5f, 1.4f);
		world.playSound(playerLoc, Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.PLAYERS, 0.5f, 1.1f);
		world.playSound(playerLoc, Sound.BLOCK_GRINDSTONE_USE, SoundCategory.PLAYERS, 0.5f, 2f);

		// and draw a nice leaf pattern
		Location mobLoc = LocationUtils.getHalfHeightLocation(mob);

		Vector front = LocationUtils.getDirectionTo(player.getLocation(), mobLoc);
		Vector up = VectorUtils.rotateTargetDirection(front, 0, -90).multiply(0.6);

		double width = mob.getWidth();

		Location origin = mobLoc.add(front.clone().multiply(Math.min(width * 1.35, 1.6)));
		Location loc1 = origin.clone().add(VectorUtils.rotateTargetDirection(up, -60, 0));
		Location loc2 = origin.clone().add(VectorUtils.rotateTargetDirection(up, -20, 0));
		Location loc3 = origin.clone().add(VectorUtils.rotateTargetDirection(up, 20, 0));
		Location loc4 = origin.clone().add(VectorUtils.rotateTargetDirection(up, 60, 0));
		Location loc5 = origin.clone().add(VectorUtils.rotateTargetDirection(up, -40, 0).multiply(1.8));
		Location loc6 = origin.clone().add(up.clone().multiply(1.8));
		Location loc7 = origin.clone().add(VectorUtils.rotateTargetDirection(up, 40, 0).multiply(1.8));

		switch (cooldowns) {
			case 0 -> { }
			case 1 -> {
				new PPLine(Particle.REDSTONE, origin, loc2).data(NEON_SMALL).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, origin, loc3).data(NEON_SMALL).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
					new PPLine(Particle.REDSTONE, loc2, loc6).data(NEON_SMALL).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
					new PPLine(Particle.REDSTONE, loc3, loc6).data(NEON_SMALL).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
				}, 1);
			}
			case 2 -> {
				new PPLine(Particle.REDSTONE, origin, loc1).data(NEON_SMALL).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, origin, loc2).data(NEON_SMALL).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, origin, loc3).data(NEON_SMALL).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, origin, loc4).data(NEON_SMALL).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
					new PPLine(Particle.REDSTONE, loc1, loc5).data(NEON_SMALL).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
					new PPLine(Particle.REDSTONE, loc2, loc5).data(NEON_SMALL).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
					new PPLine(Particle.REDSTONE, loc3, loc7).data(NEON_SMALL).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
					new PPLine(Particle.REDSTONE, loc4, loc7).data(NEON_SMALL).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
				}, 1);
			}
			default -> {
				new PPLine(Particle.REDSTONE, origin, loc1).data(NEON_SMALL).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, origin, loc2).data(NEON_SMALL).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, origin, loc3).data(NEON_SMALL).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, origin, loc4).data(NEON_SMALL).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
					new PPLine(Particle.REDSTONE, loc1, loc5).data(NEON_SMALL).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
					new PPLine(Particle.REDSTONE, loc2, loc5).data(NEON_SMALL).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
					new PPLine(Particle.REDSTONE, loc2, loc6).data(NEON_SMALL).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
					new PPLine(Particle.REDSTONE, loc3, loc6).data(NEON_SMALL).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
					new PPLine(Particle.REDSTONE, loc3, loc7).data(NEON_SMALL).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
					new PPLine(Particle.REDSTONE, loc4, loc7).data(NEON_SMALL).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
				}, 1);
			}
		}
	}

	@Override
	public void onEffectApplication(Player player, Entity entity) {
		new PartialParticle(Particle.TOTEM, LocationUtils.getHalfHeightLocation(entity), 3, 0.5, 0.5, 0.5, 0.15).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, LocationUtils.getHalfHeightLocation(entity), 3, 0.5, 0.5, 0.5).data(NEON).spawnAsPlayerActive(player);
	}

	@Override
	public void onReleaseStoredEffects(Player player, Entity entity, double radius) {
		World world = player.getWorld();
		Location loc = player.getLocation();

		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.0f, 0.5f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.0f, 0.75f);
		world.playSound(loc, Sound.ENTITY_GLOW_SQUID_DEATH, SoundCategory.PLAYERS, 1.0f, 2.0f);

		new BukkitRunnable() {
			int mTicks = 0;
			double mRadius = 0.5;
			final Location mLoc = entity.getLocation();
			double mAngle = FastUtils.randomDoubleInRange(0, 90);
			final double mRotation = FastUtils.randomDoubleInRange(10, 45) * (FastUtils.RANDOM.nextBoolean() ? 1 : -1);

			@Override
			public void run() {
				Location loc1 = mLoc.clone().add(VectorUtils.rotateYAxis(new Vector(mRadius, 0, 0), mAngle));
				Location loc2 = mLoc.clone().add(VectorUtils.rotateYAxis(new Vector(mRadius, 0, 0), mAngle + 90));
				Location loc3 = mLoc.clone().add(VectorUtils.rotateYAxis(new Vector(mRadius, 0, 0), mAngle + 180));
				Location loc4 = mLoc.clone().add(VectorUtils.rotateYAxis(new Vector(mRadius, 0, 0), mAngle + 270));
				Particle.DustOptions dustOptions = ParticleUtils.getTransition(TEAL, NEON, mRadius / radius);

				new PPLine(Particle.REDSTONE, loc1, loc2).data(dustOptions).countPerMeter(10).groupingDistance(0).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, loc2, loc3).data(dustOptions).countPerMeter(10).groupingDistance(0).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, loc3, loc4).data(dustOptions).countPerMeter(10).groupingDistance(0).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, loc4, loc1).data(dustOptions).countPerMeter(10).groupingDistance(0).spawnAsPlayerActive(player);

				mTicks++;
				mRadius += 0.5;
				mAngle += mRotation;
				if (mTicks > 20 || mRadius > radius) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void onStoreEffects(Player player, World world, Location loc, LivingEntity entity) {
		// literally too perfect to change
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.6f, 1.65f);

		createOrb(new Vector(FastUtils.randomDoubleInRange(-0.75, 0.75),
			FastUtils.randomDoubleInRange(1, 1.75),
			FastUtils.randomDoubleInRange(-0.75, 0.75)), loc.clone().add(0, 1, 0), player, entity, null);
	}

	private void createOrb(Vector dir, Location loc, Player player, LivingEntity target, @Nullable Location optLoc) {
		World world = loc.getWorld();
		new BukkitRunnable() {
			final Location mL = target.getLocation().clone();
			int mT = 0;
			double mArcCurve = 0;
			Vector mD = dir.clone();

			@Override
			public void run() {
				mT++;

				Location to = optLoc != null ? optLoc : LocationUtils.getHalfHeightLocation(player);

				for (int i = 0; i < 4; i++) {
					if (mT <= 2) {
						mD = dir.clone();
					} else {
						mArcCurve += 0.085;
						mD = dir.clone().add(LocationUtils.getDirectionTo(to, mL).multiply(mArcCurve));
					}

					if (mD.length() > 0.2) {
						mD.normalize().multiply(0.2);
					}

					mL.add(mD);

					for (int j = 0; j < 2; j++) {
						Color c = FastUtils.RANDOM.nextBoolean() ? NEON.getColor() : TEAL.getColor();
						double red = c.getRed() / 255.0;
						double green = c.getGreen() / 255.0;
						double blue = c.getBlue() / 255.0;
						new PartialParticle(Particle.SPELL_MOB, mL.clone(), 1, red, green, blue, 1).directionalMode(true).spawnAsPlayerActive(player);
					}
					if (i % 2 == 0) {
						new PartialParticle(Particle.SCRAPE, mL, 1).extra(9)
							.delta(mD.getX(), mD.getY(), mD.getZ()).directionalMode(true).spawnAsPlayerActive(player);
					}


					if (mT > 5 && mL.distance(to) < 0.35) {
						world.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, SoundCategory.PLAYERS, 1, 0.8f);
						new PartialParticle(Particle.SCRAPE, mL, 8, 0, 9).spawnAsPlayerActive(player);
						new PartialParticle(Particle.SPELL_INSTANT, mL, 3, 0.1, 0).spawnAsPlayerActive(player);

						this.cancel();
						return;
					}
				}

				if (mT >= 100) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
