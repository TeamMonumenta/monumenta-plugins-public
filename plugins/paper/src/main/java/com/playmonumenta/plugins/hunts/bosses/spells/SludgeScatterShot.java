package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.ExperimentSeventyOne;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SludgeScatterShot extends Spell {
	private static final int TELEGRAPH_TIME = 35;

	private static final double RADIUS = 3.5;
	private static final int IMPACT_LOCATIONS = 7;

	private static final double ANGLE_VARIATION = 0.075;

	private static final double RANGE_MAX = 16;
	private static final double RANGE_MIN = 6;

	private static final int ATTACK_DAMAGE = 90;

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

		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_HOGLIN_CONVERTED_TO_ZOMBIFIED, SoundCategory.HOSTILE, 2f, 0.8f);

		List<Location> impactLocations = new ArrayList<>();
		for (int i = 0; i < IMPACT_LOCATIONS; i++) {
			double r = FastUtils.randomDoubleInRange(RANGE_MIN, RANGE_MAX);
			double theta = i * (Math.PI * 2 / IMPACT_LOCATIONS) + (FastUtils.randomDoubleInRange(0, Math.PI * 2 / IMPACT_LOCATIONS) * FastUtils.randomDoubleInRange(1 - ANGLE_VARIATION, 1 + ANGLE_VARIATION));

			Location baseLocation = mBoss.getLocation().clone().add(FastUtils.cos(theta) * r, 0, FastUtils.sin(theta) * r);
			Location targetLocation = LocationUtils.mapToGround(baseLocation, 5);

			impactLocations.add(targetLocation);
		}

		Collections.shuffle(impactLocations);
		int players = PlayerUtils.playersInRange(mExperimentSeventyOne.mSpawnLoc, ExperimentSeventyOne.OUTER_RADIUS, true).size();
		int spawners = (players >= ExperimentSeventyOne.HIGH_PLAYER_CUTOFF ? HIGH_PLAYER_WORM_SPAWNERS : WORM_SPAWNERS);
		for (Location location : impactLocations) {
			createShot(location, impactLocations.indexOf(location) < spawners);
		}

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				// initial sounds
				if (mTicks < 12 && mTicks % 2 == 0) {
					mWorld.playSound(mBoss.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.HOSTILE, 2f, 0.8f);
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.HOSTILE, 1.3f, 0.9f);
				}

				if (mTicks == TELEGRAPH_TIME - 10) {
					mWorld.playSound(mBoss.getLocation().clone().add(0, 6, 0), Sound.ENTITY_HORSE_BREATHE, SoundCategory.HOSTILE, 3f, 0.8f);
				}

				// impact
				if (mTicks == TELEGRAPH_TIME) {
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 5f, 0.7f);
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 5f, 1.3f);
				}

				mTicks++;
				if (mTicks > TELEGRAPH_TIME || mBoss.isDead()) {
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();

				EntityUtils.cancelSelfRoot(mBoss);
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	private void createShot(Location targetLocation, boolean placeWormSpawner) {
		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks % 4 == 0) {
					double randomRot = FastUtils.randomDoubleInRange(0, 1);

					new PPParametric(Particle.REDSTONE, targetLocation, (parameter, builder) -> {
						double theta = (parameter + randomRot) * Math.PI * 2;

						Location location = targetLocation.clone().add(FastUtils.cos(theta) * RADIUS, 0, FastUtils.sin(theta) * RADIUS);
						Location finalLocation = LocationUtils.fallToGround(location.clone().add(0, 3, 0), 6).add(0, 0.25, 0);
						builder.location(finalLocation);
					})
						.count((int) (Math.PI * 2 * RADIUS))
						.data(new Particle.DustOptions(Color.fromRGB(161, 79, 43), 2.25f))
						.spawnAsBoss();

					new PPParametric(Particle.BLOCK_CRACK, targetLocation, (parameter, builder) -> {
						double r = FastUtils.randomDoubleInRange(0, RADIUS);
						double theta = FastUtils.randomDoubleInRange(0, Math.PI * 2);

						Location location = targetLocation.clone().add(FastUtils.cos(theta) * r, 0, FastUtils.sin(theta) * r);
						Location finalLocation = LocationUtils.fallToGround(location.clone().add(0, 3, 0), 6).add(0, 0.125, 0);
						builder.location(finalLocation);
					})
						.data(Material.MUD.createBlockData())
						.count(25)
						.spawnAsBoss();
				}

				if (mTicks == TELEGRAPH_TIME) {
					new PPParametric(Particle.SMOKE_LARGE, targetLocation, (parameter, builder) -> {
						double theta = parameter * Math.PI * 2;

						Location location = targetLocation.clone().add(FastUtils.cos(theta) * RADIUS, 0, FastUtils.sin(theta) * RADIUS);
						Location finalLocation = LocationUtils.fallToGround(location.clone().add(0, 3, 0), 6).add(0, 0.25, 0);
						builder.location(finalLocation);
						builder.offset(0, FastUtils.randomDoubleInRange(0.7, 1.3), 0);
					})
						.count((int) (Math.PI * 2 * RADIUS))
						.extra(0.2)
						.directionalMode(true)
						.spawnAsBoss();

					Hitbox hitbox = new Hitbox.UprightCylinderHitbox(targetLocation, 10, RADIUS);
					for (Player player : hitbox.getHitPlayers(true)) {
						player.playSound(player.getLocation(), Sound.BLOCK_HONEY_BLOCK_BREAK, SoundCategory.HOSTILE, 1.5f, 0.9f);
						// 50% magic and 50% blast
						DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, (double) ATTACK_DAMAGE / 2, null, true, true, "Sludge Scatter Shot");
						DamageUtils.damage(mBoss, player, DamageEvent.DamageType.BLAST, (double) ATTACK_DAMAGE / 2, null, true, true, "Sludge Scatter Shot");
					}

					List<Block> mudBlocks = new ArrayList<>(BlockUtils.getBlocksInSphere(targetLocation, RADIUS).stream().filter(Block::isSolid).toList());
					Collections.shuffle(mudBlocks);
					if (placeWormSpawner) {
						mExperimentSeventyOne.placeWormSpawner(mudBlocks.remove(0));
					}
					if (mPermanentMud) {
						mudBlocks.forEach(mExperimentSeventyOne::placeMudBlock);
					} else {
						mudBlocks.forEach((block) -> mExperimentSeventyOne.placeMudBlock(block, MUD_TIME));
					}
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

	@Override
	public int cooldownTicks() {
		return 30 + TELEGRAPH_TIME + mCooldownModifier;
	}
}
