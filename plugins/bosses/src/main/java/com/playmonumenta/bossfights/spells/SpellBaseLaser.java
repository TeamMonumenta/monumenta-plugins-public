package com.playmonumenta.bossfights.spells;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SplittableRandom;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.bossfights.utils.Utils;

public class SpellBaseLaser extends Spell {
	@FunctionalInterface
	public interface TickAction {
		/**
		 * User function called once every two ticks while laser is running
		 * @param player  Player being targeted
		 * @param tick    Number of ticks since start of attack
		 *      NOTE - Only even numbers are returned here!
		 * @param blocked Whether the laser is obstructed (true) or hits the player (false)
		 */
		void run(Player player, int tick, boolean blocked);
	}

	@FunctionalInterface
	public interface ParticleAction {
		/**
		 * User function called many times per tick with the location where
		 * a laser particle should be spawned
		 * @param loc Location to spawn a particle
		 */
		void run(Location loc);
	}

	@FunctionalInterface
	public interface FinishAction {
		/**
		 * User function called once every two ticks while laser is running
		 * @param player  Player being targeted
		 * @param loc     Location where the laser ends (either at player or occluding block)
		 * @param blocked Whether the laser is obstructed (true) or hits the player (false)
		 */
		void run(Player player, Location loc, boolean blocked);
	}

	private final Plugin mPlugin;
	private final Entity mBoss;
	private final int mRange;
	private final int mNumTicks;
	private final boolean mStopWhenBlocked;
	private final boolean mSingleTarget;
	private final int mCooldown;
	private final TickAction mTickAction;
	private final ParticleAction mParticleAction;
	private final FinishAction mFinishAction;
	private final SplittableRandom mRandom = new SplittableRandom();

	/**
	 * @param plugin          Plugin
	 * @param boss            Boss
	 * @param range           Range within which players may be targeted
	 * @param numTicks        Total duration of the spell
	 * @param stopWhenBlocked Whether the spell should abort if line of sight is broken
	 * @param singleTarget    Target random player (true) or all players (false)
	 * @param cooldown        How often this spell can be cast
	 * @param tickAction      Called once every two ticks for targeted player(s)
	 * @param particleAction  Called many times per tick to generate particles for laser
	 * @param finishAction    Called when the spell numTicks have elapsed
	 */
	public SpellBaseLaser(Plugin plugin, Entity boss, int range, int numTicks, boolean stopWhenBlocked, boolean singleTarget, int cooldown,
	                      TickAction tickAction, ParticleAction particleAction, FinishAction finishAction) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mNumTicks = numTicks;
		mStopWhenBlocked = stopWhenBlocked;
		mSingleTarget = singleTarget;
		mCooldown = cooldown;
		mTickAction = tickAction;
		mParticleAction = particleAction;
		mFinishAction = finishAction;
	}

	@Override
	public void run() {
		List<Player> players = Utils.playersInRange(mBoss.getLocation(), mRange);
		if (!players.isEmpty()) {
			if (mSingleTarget) {
				// Single target chooses a random player within range
				Collections.shuffle(players);
				for (Player player : players) {
					if (Utils.hasLineOfSight(player, mBoss)) {
						launch(player);
						return;
					}
				}
			} else {
				// Otherwise target all players within range
				for (Player player : players) {
					launch(player);
				}
			}
		}
	}

	@Override
	public boolean canRun() {
		List<Player> players = Utils.playersInRange(mBoss.getLocation(), mRange);
		if (!players.isEmpty()) {
			for (Player player : players) {
				if (Utils.hasLineOfSight(player, mBoss)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public int duration() {
		return mCooldown;
	}

	private void launch(Player target) {
		BukkitRunnable runnable = new BukkitRunnable() {
			private int mTicks = 0;

			@Override
			public void run() {
				Location launLoc = mBoss.getLocation().add(0, 1.6f, 0);
				Location tarLoc = target.getEyeLocation();
				Location endLoc = launLoc;
				BoundingBox box = BoundingBox.of(endLoc, 0.5, 0.5, 0.5);

				Vector baseVect = new Vector(tarLoc.getX() - launLoc.getX(), tarLoc.getY() - launLoc.getY(), tarLoc.getZ() - launLoc.getZ());
				baseVect = baseVect.normalize().multiply(0.5);

				boolean blocked = false;
				for (int i = 0; i < 200; i++) {
					box.shift(baseVect);
					endLoc = box.getCenter().toLocation(mBoss.getWorld());

					if (mParticleAction != null && mRandom.nextInt(3) == 0) {
						mParticleAction.run(endLoc);
					}
					List<Block> blocks = new ArrayList<Block>();
					for (int x = -1; x < 1; x++) {
						for (int y = -1; y < 1; y++) {
							for (int z = -1; z < 1; z++) {
								blocks.add(endLoc.clone().add(x, y, z).getBlock());
							}
						}
					}

					boolean cancel = false;
					for (Block block : blocks) {
						if (block.getBoundingBox().overlaps(box)) {
							cancel = true;
							break;
						}
					}

					if (cancel) {
						blocked = true;
						break;
					}
					if (endLoc.getBlock().getType().isSolid()) {
						blocked = true;
						break;
					} else if (launLoc.distance(endLoc) > launLoc.distance(tarLoc)) {
						break;
					} else if (tarLoc.distance(endLoc) < 0.5) {
						break;
					}
				}

				if (blocked && mStopWhenBlocked) {
					this.cancel();
					mActiveRunnables.remove(this);
					return;
				}

				if (mTickAction != null) {
					mTickAction.run(target, mTicks, blocked);
				}

				if (mTicks >= mNumTicks) {
					if (mFinishAction != null) {
						mFinishAction.run(target, endLoc, blocked);
					}

					this.cancel();
					mActiveRunnables.remove(this);
					return;
				}

				mTicks += 2;
			}
		};

		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);
	}
}
