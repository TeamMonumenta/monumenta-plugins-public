package com.playmonumenta.plugins.cosmetics.skills.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class BurstingRootsCS extends GraspingClawsCS {
	private static final Particle.DustOptions BROWN = new Particle.DustOptions(Color.fromRGB(115, 65, 15), 1f);
	private static final BlockData BROWN_CONCRETE = Material.BROWN_CONCRETE.createBlockData();
	private static final BlockData BROWN_TERRACOTTA = Material.BROWN_TERRACOTTA.createBlockData();
	private static final BlockData GREEN_BLOCK = Material.GREEN_CONCRETE.createBlockData();

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Gnarled roots erupt at your grasp,",
			"piercing, impaling, enchaining."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.DEAD_BUSH;
	}

	@Override
	public @Nullable String getName() {
		return "Bursting Roots";
	}

	@Override
	public void onLand(Player player, World world, Location loc, double radius) {
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.5f, 0.65f);
		world.playSound(loc, Sound.BLOCK_GRASS_BREAK, SoundCategory.PLAYERS, 1.5f, 0.5f);
		world.playSound(loc, Sound.BLOCK_GRASS_BREAK, SoundCategory.PLAYERS, 1.5f, 1.0f);
		world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 2.0f, 0.7f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 0.8f, 0.5f);
		world.playSound(loc, Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.PLAYERS, 0.8f, 0.5f);
		world.playSound(loc, Sound.ENTITY_WARDEN_DEATH, SoundCategory.PLAYERS, 0.6f, 1.0f);

		// draw lines to each mob
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, radius);
		mobs.removeIf(e -> e.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
		int mobCount = 0;
		for (LivingEntity mob : mobs) {
			drawSpike(player, loc, LocationUtils.getEntityCenter(mob));

			mobCount++;
			if (mobCount >= 8) { // cap max mob lines at 8 to prevent issues with tons of mobs
				break;
			}
		}

		// draw random lines as well
		for (int i = 0; i < 12; i++) {
			double length = FastUtils.randomDoubleInRange(radius * 0.4, radius * 0.8);
			double yaw = 30 * i;
			double pitch = FastUtils.randomDoubleInRange(0, 60) * (i % 2 == 0 ? 1 : -1);
			Location target = loc.clone().add(VectorUtils.rotateTargetDirection(new Vector(length, 0, 0), yaw, pitch));
			Location start = loc.clone().add(FastUtils.randomDoubleInRange(-0.25, 0.25), FastUtils.randomDoubleInRange(-0.25, 0.25), FastUtils.randomDoubleInRange(-0.25, 0.25));
			drawSpike(player, start, target);
		}
	}

	@Override
	public void onCageCreation(World world, Location loc) {
		world.playSound(loc, Sound.BLOCK_CHEST_OPEN, SoundCategory.PLAYERS, 1.0f, 0.65f);
		world.playSound(loc, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 1.0f, 0.5f);
	}

	@Override
	public void onCagedMob(Player player, World world, Location loc, LivingEntity mob) {
		world.playSound(loc, Sound.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 0.75f, 0.65f);
	}

	@Override
	public void cageTick(Player player, Location loc, double radius, int ticks) {
		new PartialParticle(Particle.FALLING_SPORE_BLOSSOM, loc.clone().add(0, 5, 0), 4).delta(radius / 2).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FALLING_DUST, loc.clone().add(0, 5, 0), 4).data(GREEN_BLOCK)
			.delta(radius / 2).spawnAsPlayerActive(player);

		if (ticks % 5 == 0) {
			new PPCircle(Particle.FALLING_DUST, loc.clone().add(0, 5, 0), radius).data(BROWN_CONCRETE)
				.delta(0.05).countPerMeter(1.5).spawnAsPlayerActive(player);
		}

		if (ticks == 1 || ticks % 20 == 0) {
			for (double degree = 0; degree < 360; degree += 20) {
				double offset = degree;

				new PPParametric(Particle.FALLING_DUST, loc, (param, builder) -> {
					double x = FastUtils.cosDeg(param * 20) * radius;
					double y = param * 5;
					double z = FastUtils.sinDeg(param * 20) * radius;
					Vector vec = new Vector(x, y, z);
					vec = VectorUtils.rotateYAxis(vec, offset);
					builder.location(loc.clone().add(vec));
					builder.data(FastUtils.RANDOM.nextBoolean() ? BROWN_CONCRETE : BROWN_TERRACOTTA);
				}).count(30).spawnAsPlayerActive(player);

				new PPParametric(Particle.FALLING_DUST, loc, (param, builder) -> {
					double x = FastUtils.cosDeg(param * 20) * radius;
					double y = param * 5;
					double z = -FastUtils.sinDeg(param * 20) * radius;
					Vector vec = new Vector(x, y, z);
					vec = VectorUtils.rotateYAxis(vec, offset);
					builder.location(loc.clone().add(vec));
					builder.data(FastUtils.RANDOM.nextBoolean() ? BROWN_CONCRETE : BROWN_TERRACOTTA);
				}).count(30).spawnAsPlayerActive(player);
			}
		}
	}

	@Override
	public void cleaveReadyTick(Player player) {
		Location rightHand = PlayerUtils.getRightSide(player.getEyeLocation(), 0.45).subtract(0, .8, 0);
		Location leftHand = PlayerUtils.getRightSide(player.getEyeLocation(), -0.45).subtract(0, .8, 0);
		new PartialParticle(Particle.CRIT, leftHand, 4, 0.1f, 0.1f, 0.1f, 0).spawnAsPlayerPassive(player);
		new PartialParticle(Particle.CRIT, rightHand, 4, 0.1f, 0.1f, 0.1f, 0).spawnAsPlayerPassive(player);
	}

	@Override
	public void onCleaveHit(Player player, LivingEntity mob, double radius) {
		World world = mob.getWorld();
		Location loc = mob.getLocation();

		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.0f, 0.5f);
		world.playSound(loc, Sound.BLOCK_GRASS_BREAK, SoundCategory.PLAYERS, 1.0f, 1.2f);
		world.playSound(loc, Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.0f, 0.8f);
		world.playSound(loc, Sound.ENTITY_WITHER_HURT, SoundCategory.PLAYERS, 0.7f, 0.7f);
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 1.0f, 0.5f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0f, 2.0f);
		world.playSound(loc, Sound.ENTITY_WARDEN_DEATH, SoundCategory.PLAYERS, 1.0f, 2.0f);

		for (int i = 0; i < 15; i++) {
			double length = FastUtils.randomDoubleInRange(radius * 0.6, radius * 1.2);
			double yaw = 24 * i;
			double pitch = FastUtils.randomDoubleInRange(0, 30) * (i % 2 == 0 ? 1 : -1);
			Location target = LocationUtils.getHalfHeightLocation(mob).add(VectorUtils.rotateTargetDirection(new Vector(length, 0, 0), yaw, pitch));
			Location start = LocationUtils.getHalfHeightLocation(mob).add(FastUtils.randomDoubleInRange(-0.25, 0.25), FastUtils.randomDoubleInRange(-0.1, 0.1), FastUtils.randomDoubleInRange(-0.25, 0.25));
			drawSpike(player, start, target);
		}
	}

	@Override
	public String getProjectileName() {
		return "Bursting Roots Projectile";
	}

	@Override
	public @Nullable Particle getProjectileParticle() {
		return Particle.CRIT;
	}

	private void drawSpike(Player player, Location origin, Location target) {
		double velocity = Math.max(0.22, origin.distance(target) * 0.04);
		new BukkitRunnable() {
			int mTicks = 0;
			int mIter = 0;
			final Location mLoc = origin.clone();

			@Override
			public void run() {
				for (int i = 0; i < 6; i++) {
					Vector dir = LocationUtils.getDirectionTo(target, mLoc);

					new PartialParticle(Particle.REDSTONE, mLoc, 2).data(BROWN)
						.delta(0.03).spawnAsPlayerActive(player);
					new PartialParticle(Particle.BLOCK_CRACK, mLoc, 2).data(Material.JUNGLE_WOOD.createBlockData())
						.delta(0.03).spawnAsPlayerActive(player);
					new PartialParticle(Particle.CRIT, mLoc, 1)
						.directionalMode(true).delta(dir.getX(), dir.getY(), dir.getZ()).extra(1).spawnAsPlayerActive(player);


					mLoc.add(dir.clone().multiply(velocity));

					if (mLoc.distance(target) < 0.5) {
						this.cancel();
					}
					mIter++;
				}

				if (mTicks >= 20) {
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
