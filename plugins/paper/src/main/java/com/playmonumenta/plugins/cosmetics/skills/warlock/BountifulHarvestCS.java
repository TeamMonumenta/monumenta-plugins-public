package com.playmonumenta.plugins.cosmetics.skills.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
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

public class BountifulHarvestCS extends SanguineHarvestCS {
	private static final Particle.DustTransition BEAM_TRANSITION = new Particle.DustTransition(
		Color.fromRGB(190, 240, 90),
		Color.fromRGB(255, 190, 0),
		1f);

	private static final Particle.DustTransition BEAM_TRANSITION_SMALL = new Particle.DustTransition(
		Color.fromRGB(190, 240, 90),
		Color.fromRGB(255, 190, 0),
		0.7f);
	private static final Particle.DustOptions GOLD = new Particle.DustOptions(Color.fromRGB(245, 180, 40), 1f);
	private static final Particle.DustOptions GOLD_SMALL = new Particle.DustOptions(Color.fromRGB(245, 180, 40), 0.7f);
	private static final Particle.DustOptions ORANGE = new Particle.DustOptions(Color.fromRGB(245, 225, 40), 1f);
	private static final Particle.DustOptions RED = new Particle.DustOptions(Color.fromRGB(185, 70, 40), 1f);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"The key to a bountiful harvest is a good fertilizer.",
			"Blood should do, no?"
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.WOODEN_HOE;
	}

	@Override
	public @Nullable String getName() {
		return "Bountiful Harvest";
	}

	@Override
	public void onCast(World world, Location loc) {
		world.playSound(loc, Sound.ITEM_HOE_TILL, SoundCategory.PLAYERS, 1f, 1.2f);
		world.playSound(loc, Sound.ITEM_HOE_TILL, SoundCategory.PLAYERS, 1f, 0.5f);
		world.playSound(loc, Sound.BLOCK_SAND_HIT, SoundCategory.PLAYERS, 0.8f, 0.55f);
		world.playSound(loc, Sound.BLOCK_GRAVEL_BREAK, SoundCategory.PLAYERS, 0.8f, 0.55f);
		world.playSound(loc, Sound.BLOCK_GRASS_BREAK, SoundCategory.PLAYERS, 0.8f, 0.55f);
	}

	@Override
	public void onEnhancementCast(Player player, Location loc) {
		player.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1f, 0.8f);
	}

	@Override
	public void projectileParticle(Player player, Location startLoc, Location endLoc) {
		double distance = startLoc.distance(endLoc);
		new PPParametric(Particle.DUST_COLOR_TRANSITION, startLoc, (param, builder) -> {
			double x = 1 * 0.6 * FastUtils.sin(1.2 * distance * param + 0.7);
			double y = 0;
			double z = distance * Math.pow(param, 1.5);
			Vector vec = new Vector(x, y, z);
			vec = VectorUtils.rotateXAxis(vec, startLoc.getPitch());
			vec = VectorUtils.rotateYAxis(vec, startLoc.getYaw());

			builder.location(startLoc.clone().add(vec));
		}).data(BEAM_TRANSITION).count((int) (distance * 12)).spawnAsPlayerActive(player);

		new PPParametric(Particle.DUST_COLOR_TRANSITION, startLoc, (param, builder) -> {
			double x = -1 * 0.6 * FastUtils.sin(1.2 * distance * param + 0.7);
			double y = 0;
			double z = distance * Math.pow(param, 1.5);
			Vector vec = new Vector(x, y, z);
			vec = VectorUtils.rotateXAxis(vec, startLoc.getPitch());
			vec = VectorUtils.rotateYAxis(vec, startLoc.getYaw());

			builder.location(startLoc.clone().add(vec));
		}).data(BEAM_TRANSITION).count((int) (distance * 12)).spawnAsPlayerActive(player);
	}

	@Override
	public void onExplode(Player player, World world, Location loc, double radius) {
		new PartialParticle(Particle.TOTEM, loc, 50, 0, 0, 0, 0.65).spawnAsPlayerActive(player);
		new PartialParticle(Particle.DUST_COLOR_TRANSITION, loc, (int) (50 * radius / 4)).delta(radius * 0.5).data(BEAM_TRANSITION).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FALLING_DUST, loc, (int) (50 * radius / 4)).delta(radius * 0.5).data(Material.YELLOW_CONCRETE.createBlockData()).spawnAsPlayerActive(player);
		new PPCircle(Particle.TOTEM, loc, radius).countPerMeter(1.5).spawnAsPlayerActive(player);

		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 0.6f, 0.5f);
		world.playSound(loc, Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE, SoundCategory.PLAYERS, 0.8f, 0.6f);
	}

	@Override
	public void atMarkedLocation(Player player, Location loc) {
		double offset = FastUtils.randomDoubleInRange(0, 360);
		Location loc1 = loc.clone().add(VectorUtils.rotateYAxis(new Vector(0.2, 0, 0.2), offset));
		Location loc2 = loc.clone().add(VectorUtils.rotateYAxis(new Vector(-0.35, 0, 0.35), offset));
		Location loc3 = loc.clone().add(VectorUtils.rotateYAxis(new Vector(-0.2, 0, -0.2), offset));
		Location loc4 = loc.clone().add(VectorUtils.rotateYAxis(new Vector(0.35, 0, -0.35), offset));

		new PPLine(Particle.REDSTONE, loc1, loc2).data(GOLD_SMALL)
			.countPerMeter(8).groupingDistance(0).spawnAsPlayerActive(player);
		new PPLine(Particle.REDSTONE, loc2, loc3).data(GOLD_SMALL)
			.countPerMeter(8).groupingDistance(0).spawnAsPlayerActive(player);
		new PPLine(Particle.REDSTONE, loc3, loc4).data(GOLD_SMALL)
			.countPerMeter(8).groupingDistance(0).spawnAsPlayerActive(player);
		new PPLine(Particle.REDSTONE, loc4, loc1).data(GOLD_SMALL)
			.countPerMeter(8).groupingDistance(0).spawnAsPlayerActive(player);
	}

	@Override
	public void entityGainEffect(Entity entity) {
		Location loc = LocationUtils.getEntityCenter(entity);
		new PartialParticle(Particle.DUST_COLOR_TRANSITION, loc, 20, 0.3, 0).data(BEAM_TRANSITION).spawnAsEnemyBuff();
		new PartialParticle(Particle.REDSTONE, loc, 20, 0.3, 0).data(GOLD).spawnAsEnemyBuff();
	}

	@Override
	public void entityTickEffect(Entity mob, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		Location loc = LocationUtils.getEntityCenter(mob);
		new PartialParticle(Particle.SPORE_BLOSSOM_AIR, loc, 2, 0.25, 0.5, 0.25, 0).spawnAsEnemyBuff();
		new PartialParticle(Particle.DUST_COLOR_TRANSITION, loc, 6, 0.2, 0.4, 0.2, 0.1, BEAM_TRANSITION).spawnAsEnemyBuff();

		if (oneHertz) {
			double length = mob.getWidth() * 1.25;
			Location diamond1 = mob.getLocation().add(length, 0, 0);
			Location diamond2 = mob.getLocation().add(0, 0, length);
			Location diamond3 = mob.getLocation().add(-length, 0, 0);
			Location diamond4 = mob.getLocation().add(0, 0, -length);

			new PPLine(Particle.DUST_COLOR_TRANSITION, diamond1, diamond2).data(BEAM_TRANSITION_SMALL)
				.countPerMeter(10).groupingDistance(0).spawnAsEnemyBuff();
			new PPLine(Particle.DUST_COLOR_TRANSITION, diamond2, diamond3).data(BEAM_TRANSITION_SMALL)
				.countPerMeter(10).groupingDistance(0).spawnAsEnemyBuff();
			new PPLine(Particle.DUST_COLOR_TRANSITION, diamond3, diamond4).data(BEAM_TRANSITION_SMALL)
				.countPerMeter(10).groupingDistance(0).spawnAsEnemyBuff();
			new PPLine(Particle.DUST_COLOR_TRANSITION, diamond4, diamond1).data(BEAM_TRANSITION_SMALL)
				.countPerMeter(10).groupingDistance(0).spawnAsEnemyBuff();
		}
	}

	@Override
	public void onHurt(LivingEntity mob, Player player) {
		onDeath(mob, player);
	}

	@Override
	public void onDeath(LivingEntity mob, Player player) {
		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.5f, 2f);
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_DEATH, SoundCategory.PLAYERS, 0.5f, 2f);

		// normal particles
		new PartialParticle(Particle.REDSTONE, LocationUtils.getHalfHeightLocation(mob), 30, 0.4, 0.6, 0.4, 0.1, GOLD).spawnAsEnemyBuff();

		// and draw arc to player
		double randYaw = FastUtils.randomDoubleInRange(45, 130) * (FastUtils.RANDOM.nextBoolean() ? -1 : 1);
		double randPitch = FastUtils.randomDoubleInRange(-90, -30);
		double randSpeed = FastUtils.randomDoubleInRange(1.5, 2.75);
		Vector dir = VectorUtils.rotateTargetDirection(new Vector(randSpeed, 0, 0), randYaw, randPitch);
		new BukkitRunnable() {
			final Location mL = LocationUtils.getHalfHeightLocation(mob);
			int mT = 0;
			double mArcCurve = 0;
			Vector mD = dir.clone();

			@Override
			public void run() {
				mT++;

				for (int i = 0; i < 7; i++) {
					if (mT <= 2) {
						mD = dir.clone();
					} else {
						mArcCurve += 0.35;
						mD = dir.clone().add(LocationUtils.getDirectionTo(player.getLocation(), mL).multiply(mArcCurve));
					}

					if (mD.length() > 0.23) {
						mD.normalize().multiply(0.23);
					}

					mL.add(mD);


					new PartialParticle(Particle.REDSTONE, mL, 5).data(ParticleUtils.getTransition(ORANGE, RED, mT / 5.0))
						.delta(0.07).spawnAsPlayerActive(player);

					if (mL.distance(player.getLocation()) < 0.35) {
						new PartialParticle(Particle.HEART, LocationUtils.getHalfHeightLocation(player), 2)
							.delta(0.5).spawnAsPlayerActive(player);
						this.cancel();
					}
				}

				if (mT >= 20) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
