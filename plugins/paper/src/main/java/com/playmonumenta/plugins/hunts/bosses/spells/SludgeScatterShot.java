package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.ExperimentSeventyOne;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SludgeScatterShot extends Spell {
	private static final int TELEGRAPH_TIME = 50;

	private static final int INNER_RADIUS = 6;
	private static final int OUTER_RADIUS = 15;
	private static final int SPHERE_DELTA_Y = 2;

	private static final double RANGE_MAX = 12;
	private static final double RANGE_MIN = 5;

	private static final int ATTACK_DAMAGE = 80;

	private static final double MUD_PLACE_PROBABILITY = 0.5;

	private static final int MUD_TIME = 20 * 20;

	private static final int WORM_SPAWNERS = 1;
	private static final int HIGH_PLAYER_WORM_SPAWNERS = 2;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final ExperimentSeventyOne mExperimentSeventyOne;

	private final boolean mPermanentMud;

	private final int mCooldownModifier;

	public SludgeScatterShot(Plugin plugin, LivingEntity boss, ExperimentSeventyOne experimentSeventyOne, boolean permanentMud, int cooldownModifier) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();
		mExperimentSeventyOne = experimentSeventyOne;
		mPermanentMud = permanentMud;
		mCooldownModifier = cooldownModifier;
	}

	@Override
	public boolean canRun() {
		return mExperimentSeventyOne.canRunSpell(this);
	}

	@Override
	public void run() {
		EntityUtils.selfRoot(mBoss, TELEGRAPH_TIME);

		double r = FastUtils.randomDoubleInRange(RANGE_MIN, RANGE_MAX);
		double theta = FastUtils.randomDoubleInRange(0, Math.PI * 2);

		Location baseLocation = mBoss.getLocation().clone().add(FastUtils.cos(theta) * r, 0, FastUtils.sin(theta) * r);
		Location targetLocation = LocationUtils.mapToGround(baseLocation, 5);

		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_HOGLIN_CONVERTED_TO_ZOMBIFIED, SoundCategory.HOSTILE, 2f, 0.8f);

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				// initial sounds
				if (mTicks < 12 && mTicks % 2 == 0) {
					mWorld.playSound(mBoss.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.HOSTILE, 2f, 0.8f);
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.HOSTILE, 1.3f, 0.9f);
				}

				// target telegraph
				if (mTicks % 8 == 0) {
					telegraphTarget(targetLocation);
				}

				// pre-impact sounds
				if (mTicks == TELEGRAPH_TIME - 10) {
					mWorld.playSound(targetLocation.clone().add(0, 6, 0), Sound.ENTITY_HORSE_BREATHE, SoundCategory.HOSTILE, 3f, 0.8f);
				}

				// impact
				if (mTicks == TELEGRAPH_TIME) {
					impact(targetLocation);
				}

				mTicks++;
				if (mTicks > TELEGRAPH_TIME || mBoss.isDead()) {
					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	private void impact(Location targetLocation) {
		mWorld.playSound(targetLocation, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 5f, 0.7f);
		mWorld.playSound(targetLocation, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 5f, 1.3f);

		new PPCircle(Particle.BLOCK_CRACK, targetLocation, (float)(INNER_RADIUS + OUTER_RADIUS) / 2)
				.data(ExperimentSeventyOne.MUD_TRAIL.createBlockData())
				.ringMode(true)
				.delta((float)(OUTER_RADIUS - INNER_RADIUS) / 2)
				.countPerMeter(30)
				.spawnAsBoss();

		// get all blocks in a rectangular prism and check positions inside two spheres
		List<Block> mudBlocks = new ArrayList<>();
		for (int x = -OUTER_RADIUS; x < OUTER_RADIUS; x++) {
			for (int z = -OUTER_RADIUS; z < OUTER_RADIUS; z++) {
				for (int y = -SPHERE_DELTA_Y - 1; y < SPHERE_DELTA_Y; y++) {
					Location location = targetLocation.clone().add(x, y, z);
					double distance = location.distance(targetLocation);
					if (distance >= INNER_RADIUS && distance <= OUTER_RADIUS && FastUtils.randomDoubleInRange(0, 1) < MUD_PLACE_PROBABILITY) {
						Block block = location.getBlock();
						if (block.isSolid() && BlockUtils.isExposed(block)) {
							mudBlocks.add(block);
							Block up = block.getRelative(BlockFace.UP);
							if (!up.isSolid()) {
								up.breakNaturally();
							}
						}
					}
				}
			}
		}

		int players = PlayerUtils.playersInRange(mExperimentSeventyOne.mSpawnLoc, ExperimentSeventyOne.OUTER_RADIUS, true).size();
		for (int i = 0; i < (players >= ExperimentSeventyOne.HIGH_PLAYER_CUTOFF ? HIGH_PLAYER_WORM_SPAWNERS : WORM_SPAWNERS); i++) {
			mExperimentSeventyOne.placeWormSpawner(mudBlocks.remove(i));
		}
		if (mPermanentMud) {
			mudBlocks.forEach(mExperimentSeventyOne::placeMudBlock);
		} else {
			mudBlocks.forEach((block) -> mExperimentSeventyOne.placeMudBlock(block, MUD_TIME));
		}

		Hitbox hitbox = Hitbox.approximateHollowCylinderSegment(targetLocation.clone().subtract(0, 4, 0), 10, INNER_RADIUS, OUTER_RADIUS, Math.PI);
		for (Player player : hitbox.getHitPlayers(true)) {
			player.playSound(player.getLocation(), Sound.BLOCK_HONEY_BLOCK_BREAK, SoundCategory.HOSTILE, 1.5f, 0.9f);
			// 50% magic and 50% blast
			DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, (double) ATTACK_DAMAGE / 2, null, true, true, "Sludge Scatter Shot");
			DamageUtils.damage(mBoss, player, DamageEvent.DamageType.BLAST, (double) ATTACK_DAMAGE / 2, null, true, true, "Sludge Scatter Shot");
		}
	}

	private void telegraphTarget(Location targetLocation) {
		double randomRot = FastUtils.randomDoubleInRange(0, 1);

		new PPParametric(Particle.REDSTONE, targetLocation, (parameter, builder) -> {
			double theta = (parameter + randomRot) * Math.PI * 2;

			Location location = targetLocation.clone().add(FastUtils.cos(theta) * INNER_RADIUS, 0, FastUtils.sin(theta) * INNER_RADIUS);
			Location finalLocation = LocationUtils.fallToGround(location.clone().add(0, 3, 0), 6).add(0, 0.5, 0);
			builder.location(finalLocation);
		})
			.count((int)(Math.PI * 2 * INNER_RADIUS))
			.data(new Particle.DustOptions(Color.fromRGB(217, 37, 24), 2.25f))
			.spawnAsBoss();

		new PPParametric(Particle.REDSTONE, targetLocation, (parameter, builder) -> {
			double theta = (parameter + randomRot) * Math.PI * 2;

			Location location = targetLocation.clone().add(FastUtils.cos(theta) * OUTER_RADIUS, 0, FastUtils.sin(theta) * OUTER_RADIUS);
			Location finalLocation = LocationUtils.fallToGround(location.clone().add(0, 3, 0), 6).add(0, 0.5, 0);
			builder.location(finalLocation);
		})
			.count((int)(Math.PI * 2 * OUTER_RADIUS))
			.data(new Particle.DustOptions(Color.fromRGB(217, 37, 24), 2.25f))
			.spawnAsBoss();

		new PPParametric(Particle.BLOCK_CRACK, targetLocation, (parameter, builder) -> {
			double r = FastUtils.randomDoubleInRange(INNER_RADIUS, OUTER_RADIUS);
			double theta = FastUtils.randomDoubleInRange(0, Math.PI * 2);

			Location location = targetLocation.clone().add(FastUtils.cos(theta) * r, 0, FastUtils.sin(theta) * r);
			Location finalLocation = LocationUtils.fallToGround(location.clone().add(0, 3, 0), 6).add(0, 0.25, 0);
			builder.location(finalLocation);
		})
			.data(Material.MUD.createBlockData())
			.count(1000)
			.spawnAsBoss();
	}

	@Override
	public int cooldownTicks() {
		return 30 + TELEGRAPH_TIME + mCooldownModifier;
	}
}
