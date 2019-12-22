package com.playmonumenta.bossfights.spells;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.bossfights.utils.Utils;

public class SpellBaseCharge extends Spell {
	@FunctionalInterface
	public interface WarningAction {
		/**
		 * Action to notify player when the boss starts the attack
		 *
		 * Probably slow the boss down, particles, and a sound
		 *
		 * @param player Targeted player
		 */
		void run(Player player);
	}

	@FunctionalInterface
	public interface WarningParticles {
		/**
		 * Particles to indicate the path of the boss's charge
		 *
		 * @param loc Location to spawn a particle
		 */
		void run(Location loc);
	}

	@FunctionalInterface
	public interface StartAction {
		/**
		 * Action run when the boss begins the attack
		 * Boss location will be the origin point
		 *
		 * Probably Particles / sound
		 *
		 * @param player Targeted player
		 */
		void run(Player player);
	}

	@FunctionalInterface
	public interface HitPlayerAction {
		/**
		 * Action to take when a player is hit by the boss charge
		 *
		 * Probably particles, sound, and damage player
		 *
		 * @param player Hit player
		 */
		void run(Player player);
	}

	@FunctionalInterface
	public interface ParticleAction {
		/**
		 * User function called many times per tick with the location where
		 * the boss's charge is drawn like a laser
		 *
		 * @param loc Location to spawn a particle
		 */
		void run(Location loc);
	}

	@FunctionalInterface
	public interface EndAction {
		/**
		 * Action to run on the boss when the attack is completed
		 * Boss location will be the end point
		 *
		 * Probably just particles
		 */
		void run();
	}

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private int mRange;
	private int mChargeTicks;
	private WarningAction mWarningAction;
	private ParticleAction mWarnParticleAction;
	private StartAction mStartAction;
	private HitPlayerAction mHitPlayerAction;
	private ParticleAction mParticleAction;
	private EndAction mEndAction;
	private boolean mStopOnFirstHit;
	private int mCharges;
	private int mRate;

	public SpellBaseCharge(Plugin plugin, LivingEntity boss, int range, int chargeTicks,
	                       WarningAction warning, ParticleAction warnParticles, StartAction start,
	                       HitPlayerAction hitPlayer, ParticleAction particle, EndAction end) {
		this(plugin, boss, range, chargeTicks, false, 0, 0, warning, warnParticles, start, hitPlayer, particle, end);
	}

	public SpellBaseCharge(Plugin plugin, LivingEntity boss, int range, int chargeTicks, boolean stopOnFirstHit,
	                       WarningAction warning, ParticleAction warnParticles, StartAction start,
	                       HitPlayerAction hitPlayer, ParticleAction particle, EndAction end) {
		this(plugin, boss, range, chargeTicks, stopOnFirstHit, 0, 0, warning, warnParticles, start, hitPlayer, particle, end);
	}

	public SpellBaseCharge(Plugin plugin, LivingEntity boss, int range, int chargeTicks, boolean stopOnFirstHit, int charges, int rate,
	                       WarningAction warning, ParticleAction warnParticles, StartAction start,
	                       HitPlayerAction hitPlayer, ParticleAction particle, EndAction end) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mChargeTicks = chargeTicks;
		mWarningAction = warning;
		mWarnParticleAction = warnParticles;
		mStartAction = start;
		mHitPlayerAction = hitPlayer;
		mParticleAction = particle;
		mEndAction = end;
		mStopOnFirstHit = stopOnFirstHit;
		mCharges = charges;
		mRate = rate;
	}

	@Override
	public void run() {
		// Get list of all nearby players who could be hit by the attack
		List<Player> bystanders = Utils.playersInRange(mBoss.getLocation(), mRange * 2);

		// Choose random player within range that has line of sight to boss
		List<Player> players = Utils.playersInRange(mBoss.getLocation(), mRange);
		Collections.shuffle(players);
		for (Player player : players) {
			if (Utils.hasLineOfSight(mBoss.getEyeLocation(), player)) {
				if (mCharges <= 0 || mRate <= 0) {
					launch(player, bystanders);
				} else {
					launch(player, bystanders, mCharges, mRate);
				}
				break;
			}
		}
	}

	@Override
	public int duration() {
		return 160; // 8 seconds
	}

	/**
	 * Does a charge attack - which may not do anything, depending on parameters passed
	 * Returns whether the charge hit a player or not
	 *
	 * @param target The intended target of the attack
	 * @param charger The living entity charging the player
	 * @param validTargets Other players (including the target!) who might be indicentally hit by the charge
	 * @param start Action to run on boss at start location (may be null)
	 * @param particle Action to spawn particle at locations along path (may be null)
	 * @param hitPlayer Action to run if a player is hit (may be null)
	 * @param end Action to run on boss at end location (may be null)
	 * @param teleboss Boolean indicating whether the boss should actually be teleported to the end
	 * @param stopOnFirstHit Boolean indicating whether the boss should damage only one player at a time
	 */
	public static boolean doCharge(Player target, Entity charger, Location targetLoc, List<Player> validTargets, StartAction start,
	                               ParticleAction particle, HitPlayerAction hitPlayer, EndAction end, boolean teleBoss, boolean stopOnFirstHit) {
		final Location launLoc;
		if (charger instanceof LivingEntity) {
			launLoc = ((LivingEntity)charger).getEyeLocation();
		} else {
			launLoc = charger.getLocation();
		}

		/* Test locations that are iterated in the loop */
		Location endLoc = launLoc.clone();
		Location endLoc1 = launLoc.clone().add(0, 1, 0); // Same as endLoc but one block higher

		Vector baseVect = new Vector(targetLoc.getX() - launLoc.getX(), targetLoc.getY() - launLoc.getY(), targetLoc.getZ() - launLoc.getZ());
		baseVect = baseVect.normalize().multiply(0.3);

		if (start != null) {
			start.run(target);
		}

		boolean chargeHitsPlayer = false;
		boolean cancel = false;
		BoundingBox box = BoundingBox.of(endLoc, 0.15, 0.15, 0.15);
		for (int i = 0; i < 200; i++) {
			box.shift(baseVect);
			endLoc.add(baseVect);
			endLoc1.add(baseVect);

			if (particle != null) {
				particle.run(endLoc);
			}

			List<Block> blocks = new ArrayList<Block>();
			for (int x = -1; x < 1; x++) {
				for (int y = -1; y < 1; y++) {
					for (int z = -1; z < 1; z++) {
						blocks.add(endLoc.clone().add(x, y, z).getBlock());
					}
				}
			}

			for (Block block : blocks) {
				if (block.getBoundingBox().overlaps(box) && !block.isLiquid()) {
					cancel = true;
					break;
				}
			}
			if (endLoc.getBlock().getType().isSolid() || endLoc1.getBlock().getType().isSolid()) {
				// No longer air - need to go back a bit so we don't tele the boss into a block
				endLoc.subtract(baseVect.multiply(1));
				// Charge terminated at a block
				break;
			} else if (launLoc.distance(endLoc) > (launLoc.distance(targetLoc) + 6.0f)) {
				// Reached end of charge without hitting anything
				break;
			}

			for (Player player : validTargets) {
				if (player.getLocation().distance(endLoc) < 1.8F) {
					// Hit player - mark this and continue
					chargeHitsPlayer = true;

					if (hitPlayer != null) {
						hitPlayer.run(player);
					}
					if (stopOnFirstHit) {
						cancel = true;
						break;
					}
				}
			}

			if (cancel) {
				break;
			}
		}

		if (teleBoss) {
			charger.teleport(endLoc);
		}

		if (end != null) {
			end.run();
		}

		return chargeHitsPlayer;
	}

	private void launch(Player target, List<Player> players, int charges, int rate) {
		BukkitRunnable runnable = new BukkitRunnable() {
			private int mTicks = 0;
			private int charges_done = 0;
			Location targetLoc;
			List<Player> bystanders = players;
			Player mTarget = target;
			@Override
			public void run() {
				if (!mBoss.isValid() || mBoss.isDead() || mBoss == null || mBoss.hasMetadata("MobIsStunnedByEntityUtils")) {
					this.cancel();
					return;
				}
				if (mTicks == 0) {
					targetLoc = mTarget.getEyeLocation();
					if (mWarningAction != null) {
						mWarningAction.run(target);
					}
				} else if (mTicks > 0 && mTicks < mChargeTicks) {
					// This runs once every other tick while charging
					doCharge(mTarget, mBoss, targetLoc, bystanders, null, mWarnParticleAction, null, null, false, mStopOnFirstHit);
				} else if (mTicks >= mChargeTicks) {
					// Do the "real" charge attack
					doCharge(mTarget, mBoss, targetLoc, bystanders, mStartAction, mParticleAction, mHitPlayerAction,
					         mEndAction, true, mStopOnFirstHit);
					charges_done++;
					if (charges_done >= charges) {
						this.cancel();
						mActiveRunnables.remove(this);
					} else {
						// Get list of all nearby players who could be hit by the attack
						bystanders = Utils.playersInRange(mBoss.getLocation(), mRange * 2);

						// Choose random player within range that has line of sight to boss
						List<Player> players = Utils.playersInRange(mBoss.getLocation(), mRange);
						Collections.shuffle(players);
						for (Player player : players) {
							if (Utils.hasLineOfSight(mBoss.getEyeLocation(), player)) {
								mTarget = player;
								targetLoc = mTarget.getLocation().add(0, 1.0f, 0);
								break;
							}
						}
						mTicks -= rate;
					}
				}

				mTicks += 2;
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);
	}

	private void launch(Player target, List<Player> players) {
		BukkitRunnable runnable = new BukkitRunnable() {
			private int mTicks = 0;
			Location targetLoc;

			@Override
			public void run() {
				if (!mBoss.isValid() || mBoss.isDead() || mBoss == null || mBoss.hasMetadata("MobIsStunnedByEntityUtils")) {
					this.cancel();
					return;
				}
				if (mTicks == 0) {
					targetLoc = target.getLocation().add(0, 1.0f, 0);
					if (mWarningAction != null) {
						mWarningAction.run(target);
					}
				} else if (mTicks > 0 && mTicks < mChargeTicks) {
					// This runs once every other tick while charging
					doCharge(target, mBoss, targetLoc, players, null, mWarnParticleAction, null, null, false, mStopOnFirstHit);
				} else if (mTicks >= mChargeTicks) {
					// Do the "real" charge attack
					doCharge(target, mBoss, targetLoc, players, mStartAction, mParticleAction, mHitPlayerAction,
					         mEndAction, true, mStopOnFirstHit);
					this.cancel();
					mActiveRunnables.remove(this);
				}

				mTicks += 2;
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);
	}
}
