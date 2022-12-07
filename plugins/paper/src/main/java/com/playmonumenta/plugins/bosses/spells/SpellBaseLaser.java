package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.LocationUtils.TravelAction;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellBaseLaser extends Spell {
	private static final double BOX_SIZE = 0.5;
	private static final double CHECK_INCREMENT = 0.2;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mRange;
	private final int mDuration;
	private final boolean mStopWhenBlocked;
	private final boolean mSingleTarget;
	private final int mCooldown;
	private final TickAction mTickAction;
	private final ParticleAction mParticleAction;
	private final int mParticleFrequency;
	private final int mParticleChance;
	private final FinishAction mFinishAction;
	private final @Nullable GetSpellTargets<LivingEntity> mGetTargets;

	/**
	 * @param plugin          Plugin
	 * @param boss            LivingEntity casting this laser
	 * @param range           Range within which players may be targeted
	 * @param duration        Duration of laser in ticks till detonation
	 * @param stopWhenBlocked Whether laser should cancel itself
	 *                        to stop early if line of sight is broken/if stunned.
	 *                        Does not shorten Spell's cooldownTicks or castTicks
	 * @param singleTarget    Target 1 random player (true) or all players (false)
	 * @param cooldown        Cooldown in ticks including laser duration
	 *                        till boss can use next spell
	 * @param tickAction      Code to run every 2 ticks while laser is active
	 * @param particleAction  Code spawning laser particles at Location to determine appearance
	 * @param finishAction    Code to run when laser detonates
	 */
	public SpellBaseLaser(
		Plugin plugin,
		LivingEntity boss,
		int range,
		int duration,
		boolean stopWhenBlocked,
		boolean singleTarget,
		int cooldown,
		TickAction tickAction,
		ParticleAction particleAction,
		FinishAction finishAction
	) {
		this(
			plugin,
			boss,
			range,
			duration,
			stopWhenBlocked,
			singleTarget,
			cooldown,
			tickAction,
			particleAction,
			1,
			6,
			finishAction
		);
	}

	public SpellBaseLaser(
		Plugin plugin,
		LivingEntity boss,
		int range,
		int duration,
		boolean stopWhenBlocked,
		boolean singleTarget,
		int cooldown,
		TickAction tickAction,
		ParticleAction particleAction,
		int particleFrequency,
		int particleChance,
		FinishAction finishAction
	) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mDuration = duration;
		mStopWhenBlocked = stopWhenBlocked;
		mSingleTarget = singleTarget;
		mCooldown = cooldown;
		mTickAction = tickAction;
		mGetTargets = null;
		mParticleAction = particleAction;
		mParticleFrequency = particleFrequency;
		mParticleChance = particleChance;
		mFinishAction = finishAction;
	}

	public SpellBaseLaser(
		Plugin plugin,
		LivingEntity boss,
		int duration,
		boolean stopWhenBlocked,
		int cooldown,
		@Nullable GetSpellTargets<LivingEntity> getTargets,
		TickAction tickAction,
		ParticleAction particleAction,
		int particleFrequency,
		int particleChance,
		FinishAction finishAction
	) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = 0;
		mDuration = duration;
		mStopWhenBlocked = stopWhenBlocked;
		mSingleTarget = false;
		mCooldown = cooldown;
		mGetTargets = getTargets;
		mTickAction = tickAction;
		mParticleAction = particleAction;
		mParticleFrequency = particleFrequency;
		mParticleChance = particleChance;
		mFinishAction = finishAction;
	}


	@Override
	public void run() {
		if (mGetTargets != null) {
			List<? extends LivingEntity> targets = mGetTargets.getTargets();
			for (LivingEntity target : targets) {
				launch(target);
			}
			return;
		}
		List<Player> potentialTargets = PlayerUtils.playersInRange(mBoss.getLocation(), mRange, false);

		// Single-target laser chooses 1 player
		if (mSingleTarget) {
			// Shuffle so whoever checks succeed on first is random
			Collections.shuffle(potentialTargets);

			// Pick first player in sight
			// canRun() ensures there is at least 1 in sight
			for (Player target : potentialTargets) {
				if (LocationUtils.hasLineOfSight(mBoss, target)) {
					launch(target);
					return;
				}
			}
		} else {
			// Group laser chooses all players
			for (Player target : potentialTargets) {
				launch(target);
			}
		}

		// If no potential targets, nothing happens
	}

	@Override
	public boolean canRun() {
		if (mGetTargets != null) {
			return !mGetTargets.getTargets().isEmpty();
		}

		List<Player> potentialTargets = PlayerUtils.playersInRange(mBoss.getLocation(), mRange, false);

		for (Player target : potentialTargets) {
			// A different kind of line check than the laser itself
			if (LocationUtils.hasLineOfSight(mBoss, target)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}

	@Override
	public int castTicks() {
		return mDuration;
	}

	private void launch(LivingEntity target) {
		BukkitRunnable runnable = new BukkitRunnable() {
			private int mTicks = 0;

			@Override
			public void run() {
				Location startLocation = mBoss.getEyeLocation();
				Location targetedLocation = target.getLocation().add(0, target.getEyeHeight() * 3 / 5, 0);

				World world = mBoss.getWorld();
				BoundingBox movingLaserBox = BoundingBox.of(startLocation, BOX_SIZE, BOX_SIZE, BOX_SIZE);
				Vector vector = new Vector(
					targetedLocation.getX() - startLocation.getX(),
					targetedLocation.getY() - startLocation.getY(),
					targetedLocation.getZ() - startLocation.getZ()
				);

				boolean blocked = LocationUtils.travelTillObstructed(
					world,
					movingLaserBox,
					startLocation.distance(targetedLocation),
					vector,
					CHECK_INCREMENT,
					false,
					mParticleAction,
					mParticleFrequency,
					mParticleChance
				);

				if ((mStopWhenBlocked && blocked) || EntityUtils.isStunned(mBoss) || EntityUtils.isSilenced(mBoss)) {
					mBoss.setAI(true);

					this.cancel();
					mActiveRunnables.remove(this);
					return;
				}

				if (mTickAction != null) {
					mTickAction.run(target, mTicks, blocked);
				}

				if (mTicks >= mDuration) {
					if (mFinishAction != null) {
						mFinishAction.run(target, movingLaserBox.getCenter().toLocation(world), blocked);
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


	@FunctionalInterface
	public interface TickAction {
		/**
		 * Called once every 2 ticks while laser is active.
		 * Useful for things like player sound cues
		 * or initial effects when tick == 0
		 *
		 * @param target  Player being targeted
		 * @param tick    Number of ticks since start of laser.
		 *                NOTE: Only even numbers are returned here!
		 * @param blocked Whether the laser is obstructed (true) or hits the player (false)
		 */
		void run(LivingEntity target, int tick, boolean blocked);
	}

	@FunctionalInterface
	public interface ParticleAction extends TravelAction {
		/**
		 * Called many times every 2 ticks with each Location
		 * where laser particles should be spawned
		 *
		 * @param location Location to use for your particles
		 */
		@Override
		void run(Location location);
	}

	@FunctionalInterface
	public interface FinishAction {
		/**
		 * Called at the end once when the laser detonates
		 *
		 * @param target      LivingEntity being targeted
		 * @param endLocation Location where the laser ends (either at player or obstructing block)
		 * @param blocked     Whether the laser is obstructed (true) or hits the player (false)
		 */
		void run(LivingEntity target, Location endLocation, boolean blocked);
	}
}
