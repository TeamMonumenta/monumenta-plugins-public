package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.CoreElemental;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SpellRumblingEmergence extends Spell implements CoreElemental.CoreElementalBase {
	// Time needed for the boss to dig into the ground
	private static final int HIDE_TIME = 30;
	// Damage dealt to surroundings when the core emerges
	private static final int EMERGE_DAMAGE = 60;
	// Damage dealt to target player
	private static final int DAMAGE = 60;
	private boolean mOnCooldown = false;
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final CoreElemental mQuarry;
	private final Location mStartLoc;

	public SpellRumblingEmergence(Plugin plugin, LivingEntity boss, CoreElemental quarry, Location startLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mQuarry = quarry;
		mStartLoc = startLoc;
	}

	@Override
	public void run() {
		// Cooldown
		mOnCooldown = true;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mOnCooldown = false, cooldownTicks() + 20 * 2);

		mBoss.setInvulnerable(true);
		mBoss.setAI(false);
		Location finalLocation = LocationUtils.fallToGround(mBoss.getLocation(), mStartLoc.getY() - 5);
		mBoss.teleport(finalLocation);
		// Digs underground
		BukkitRunnable runnable = new BukkitRunnable() {
			int mTick = 0;

			@Override
			public void run() {
				mBoss.setRotation(mTick * 15, 0);
				mBoss.teleport(mBoss.getLocation().add(0, -0.1, 0));
				new PartialParticle(Particle.BLOCK_CRACK, finalLocation.clone().add(0, 0.5, 0))
					.count(15)
					.delta(0)
					.data(Material.CRIMSON_STEM.createBlockData())
					.spawnAsBoss();
				if (mTick++ > HIDE_TIME) {
					emerge();
					this.cancel();
					mActiveRunnables.remove(this);
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	private void emerge() {
		// Target the player the furthest away from the boss
		Player target = getTarget();
		Location pendingLocation = getEmergeLocation();
		Location emergeLocation = LocationUtils.fallToGround(pendingLocation, mStartLoc.getY() - 5);
		if (target != null) {
			emergeLocation.setDirection(LocationUtils.getDirectionTo(target.getLocation(), emergeLocation).setY(0));
		}
		// Fissure
		PassiveFissure fissure = new PassiveFissure(mPlugin, mBoss, emergeLocation, 5, 1, new int[]{4}, FISSURE_MATERIAL, LAVA_MATERIAL, EMERGE_DAMAGE, getSpellName(),
			location -> new PartialParticle(Particle.EXPLOSION_NORMAL, location.clone().add(0, 1.2, 0))
				.count(3)
				.delta(0, 1, 0)
				.extra(0.4)
				.directionalMode(true)
				.spawnAsBoss(),
			location -> {
				PlayerUtils.playersInRange(emergeLocation, 2, true).forEach(player -> MovementUtils.knockAway(mBoss, player, 0.5f, 0.3f));
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.HOSTILE, 3f, 0.72f);
			},
			mQuarry);
		fissure.trigger(0, 200);

		mBoss.teleport(emergeLocation);
		mBoss.setInvulnerable(false);
		mBoss.setAI(true);
		mBoss.setVelocity(new Vector(0, 1.1, 0));
		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (target != null) {
					chargeTowardsTarget(target);
				}
			}
		};
		runnable.runTaskLater(mPlugin, 10);
		mActiveRunnables.add(runnable);
	}

	private void chargeTowardsTarget(Player target) {
		((Mob) mBoss).setTarget(target);
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_GHAST_SHOOT, SoundCategory.HOSTILE, 3f, 1.58f);
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.HOSTILE, 1.5f, 1.42f);
		Location targetLocation = target.getLocation().clone();
		BukkitRunnable runnable = new BukkitRunnable() {
			double mDistanceSquared = mBoss.getEyeLocation().distanceSquared(targetLocation);
			boolean mFirstTick = true;
			Vector mDirection = LocationUtils.getDirectionTo(target.getLocation(), mBoss.getEyeLocation());

			@Override
			public void run() {
				// Effects
				if (mFirstTick) {
					mDirection = LocationUtils.getDirectionTo(target.getLocation(), mBoss.getEyeLocation());
					new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getEyeLocation())
						.count(30)
						.delta(0.5)
						.extra(1)
						.spawnAsBoss();
					target.playSound(target.getEyeLocation(), Sound.ENTITY_WITCH_CELEBRATE, SoundCategory.HOSTILE, 3f, 1f);
					mFirstTick = false;
				}
				new PartialParticle(Particle.LAVA, mBoss.getLocation())
					.count(6)
					.delta(0.5)
					.spawnAsBoss();

				// Dash towards player
				Vector finalVelocity = mDirection;
				mBoss.setVelocity(finalVelocity);

				// Damage player
				if (mBoss.getBoundingBox().expand(1, 1, 1).overlaps(target.getBoundingBox())) {
					// Spawn Fire
					for (Block block : LocationUtils.getNearbyBlocks(mBoss.getLocation().getBlock(), 5)) {
						if (FastUtils.randomDoubleInRange(0, 1) <= 0.2 && block.isSolid()) {
							Block up = block.getRelative(BlockFace.UP);
							if (up.isEmpty()) {
								if (TemporaryBlockChangeManager.INSTANCE.changeBlock(up, Material.FIRE, 12000)) {
									mQuarry.addChangedBlock(up);
								}
							}
						}
					}

					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2f, 1f);
					new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getEyeLocation())
						.count(20)
						.delta(0)
						.extra(0.7)
						.spawnAsEntityActive(mBoss);
					DamageUtils.damage(mBoss, target, DamageEvent.DamageType.MAGIC, DAMAGE, null, false, false, getSpellName());
					MovementUtils.knockAway(mBoss, target, 0.4f, 0.3f);
					this.cancel();
					mActiveRunnables.remove(this);
				} else if (mBoss.isOnGround() || LocationUtils.collidesWithBlocks(mBoss.getBoundingBox().expand(0.5, 0, 0.5), mBoss.getWorld())) {
					// Spawn Fire
					for (Block block : LocationUtils.getNearbyBlocks(mBoss.getLocation().getBlock(), 4)) {
						if (FastUtils.randomDoubleInRange(0, 1) <= 0.15 && block.isSolid()) {
							Block up = block.getRelative(BlockFace.UP);
							if (up.isEmpty()) {
								if (TemporaryBlockChangeManager.INSTANCE.changeBlock(up, Material.FIRE, 12000)) {
									mQuarry.addChangedBlock(up);
								}
							}
						}
					}

					ParticleUtils.explodingRingEffect(mPlugin, mBoss.getLocation(), 5, 1, 10,
						List.of(
							new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(0.5, (Location location) -> new PartialParticle(Particle.FLAME, location, 1, 0, 0, 0).spawnAsEntityActive(mBoss))
						));
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2f, 1f);
					this.cancel();
					mActiveRunnables.remove(this);
				}

				// Cancels if overshot
				double newDistanceSquared = mBoss.getEyeLocation().distanceSquared(targetLocation);
				if (newDistanceSquared > mDistanceSquared) {
					this.cancel();
					mActiveRunnables.remove(this);
				} else {
					mDistanceSquared = newDistanceSquared;
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 10, 1);
		mActiveRunnables.add(runnable);
	}

	// Get the player who is the furthest away from the Core
	@Nullable
	private Player getTarget() {
		List<Player> pendingTargets = new ArrayList<>(mQuarry.getPlayers());
		Player target = null;
		double maxDistance = 0;
		for (Player player : pendingTargets) {
			double distance = mBoss.getLocation().distance(player.getLocation());
			if (distance > maxDistance) {
				maxDistance = distance;
				target = player;
			}
		}
		return target;
	}

	private Location getEmergeLocation() {
		// Always spawn on the fissure. The location is away from centre whenever possible
		List<Block> blocks = LocationUtils.getNearbyBlocks(mStartLoc.getBlock(), 35);
		List<Block> centerBlocks = LocationUtils.getNearbyBlocks(mStartLoc.getBlock(), 5);
		blocks.removeIf(block ->
			!(block.getType() == FISSURE_MATERIAL || block.getType() == LAVA_MATERIAL)
				|| !block.isSolid()
				|| BlockUtils.isPathBlockingBlock(block.getRelative(BlockFace.UP).getType())
				|| centerBlocks.contains(block));
		centerBlocks.removeIf(block ->
			!(block.getType() == FISSURE_MATERIAL || block.getType() == LAVA_MATERIAL)
				|| !block.isSolid()
				|| BlockUtils.isPathBlockingBlock(block.getRelative(BlockFace.UP).getType()));
		if (blocks.isEmpty()) {
			blocks = centerBlocks;
		}
		if (blocks.isEmpty()) {
			// This should not happen - we check that a valid block exists in canRun()
			MMLog.warning("There were no valid blocks in SpellRumblingEmergence - using mStartLoc instead!");
			return mStartLoc;
		}
		Collections.shuffle(blocks);
		return LocationUtils.fallToGround(blocks.get(0).getLocation().add(0, 10, 0), mStartLoc.getY() - 5);
	}

	@Override
	public void cancel() {
		for (BukkitRunnable runnable : new ArrayList<>(mActiveRunnables)) {
			if (!runnable.isCancelled()) {
				runnable.cancel();
			}
		}
		mActiveRunnables.clear();

		for (BukkitTask task : new ArrayList<>(mActiveTasks)) {
			if (!task.isCancelled()) {
				task.cancel();
			}
		}
		mActiveTasks.clear();
		if (!mBoss.isDead() && mBoss.isValid()) {
			mBoss.teleport(LocationUtils.fallToGround(mBoss.getLocation().add(0, 10, 0), mStartLoc.getY() - 5));
			mBoss.setAI(true);
			mBoss.setInvulnerable(false);
		}
	}

	@Override
	public int cooldownTicks() {
		return 20 * 9;
	}

	@Override
	public String getSpellName() {
		return "Rumbling Emergence";
	}

	@Override
	public String getSpellChargePrefix() {
		return "Preparing";
	}

	@Override
	public int getChargeDuration() {
		return HIDE_TIME;
	}

	@Override
	public int getSpellDuration() {
		return 20 * 2;
	}

	@Override
	public boolean canRun() {
		return LocationUtils.hasNearbyBlock(mStartLoc, 35, List.of(FISSURE_MATERIAL, LAVA_MATERIAL))
			&& !mOnCooldown
			&& !mQuarry.mIsCastingBanish;
	}
}
