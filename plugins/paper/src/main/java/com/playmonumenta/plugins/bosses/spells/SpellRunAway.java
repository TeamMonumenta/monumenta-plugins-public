package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.BaseMovementSpeedModifyEffect;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Runs away from any entities satisfying shouldRunFrom within the specified
 * radius.
 * Intended to be run as a passive spell.
 **/
public class SpellRunAway extends Spell {

	private final LivingEntity mBoss;
	private final World mWorld;
	private final double mTriggerRadiusMeters;
	private final double mStopRadiusMeters;
	private final double mRunSpeedIncrease;
	private final Predicate<Player> mShouldRunFrom;

	private State mState;
	private int mCoweringTicks;

	public enum State {
		RUNNING,
		COWERING,
		AGGRESSIVE
	}

	// Precondition: triggerRadiusMeters <= stopRadiusMeters
	public SpellRunAway(
			LivingEntity boss,
			double triggerRadiusMeters,
			double stopRadiusMeters,
			double runSpeedIncrease,
			Predicate<Player> shouldRunFrom) {
		mBoss = boss;
		mWorld = boss.getWorld();
		mTriggerRadiusMeters = triggerRadiusMeters;
		mStopRadiusMeters = stopRadiusMeters;
		mRunSpeedIncrease = runSpeedIncrease;
		mShouldRunFrom = shouldRunFrom;

		mState = State.AGGRESSIVE;
		mCoweringTicks = 0;
	}

	private static final double EPSILON = 0.00001;

	// Returns a normalized vector in which to run.
	private Optional<Vector> getRunDirection() {
		Location bossLocation = mBoss.getLocation();
		List<Player> playersToRunFrom = PlayerUtils.playersInRange(bossLocation, mStopRadiusMeters, false).stream()
				.filter(mShouldRunFrom)
				.toList();

		if (playersToRunFrom.isEmpty()) {
			return Optional.empty();
		}

		Vector runDirection = playersToRunFrom.stream()
				.map(player -> {
					Vector playerToBoss = bossLocation.subtract(player.getLocation()).toVector();

					if (playerToBoss.lengthSquared() < EPSILON) {
						return new Vector();
					}

					// Change length to 1/length. Closer players weighted higher
					return playerToBoss.multiply(1 / playerToBoss.lengthSquared());
				})
				.reduce(new Vector(), (Vector a, Vector b) -> a.add(b))
				.setY(0);

		if (runDirection.lengthSquared() < EPSILON) {
			return Optional.empty();
		}

		runDirection.normalize();

		return Optional.of(runDirection);
	}

	@Override
	// State Transition graph: All -> Running -> Cowering -> Aggressive
	public void run() {
		Collection<Player> playersTriggering = PlayerUtils
				.playersInRange(mBoss.getLocation(), mTriggerRadiusMeters, false).stream()
				.filter(mShouldRunFrom)
				.toList();
		Optional<Vector> runDirection = getRunDirection();
		if (mState != State.RUNNING && !playersTriggering.isEmpty() && runDirection.isPresent()) {
			// All -> Running state transition
			mState = State.RUNNING;
			mCoweringTicks = 0;
			// Jump back
			mBoss.setVelocity(runDirection.get().clone().multiply(0.2).setY(0.5));
			mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_VILLAGER_HURT, 1f, 1.5f);
			mBoss.setAI(true);
			return;
		}

		if (mState == State.RUNNING) {
			if (runDirection.isEmpty()) {
				// Running -> Cowering state transition
				mState = State.COWERING;
				mBoss.setAI(false);
				Plugin.getInstance().mEffectManager.clearEffects(mBoss, "Running Away");
				return;
			}

			if (mBoss instanceof Mob bossMob) {
				bossMob.setTarget(null);
				bossMob.getPathfinder().moveTo(bossMob.getLocation().add(runDirection.get().multiply(6)));

				// Grant speed bonus while running away
				Plugin.getInstance().mEffectManager
						.addEffect(mBoss, "Running Away", new BaseMovementSpeedModifyEffect(1, mRunSpeedIncrease));
			}
		}

		if (mState == State.COWERING) {
			// Cowering -> Aggressive state transition
			if (mCoweringTicks >= 30) {
				mState = State.AGGRESSIVE;
				mBoss.setAI(true);
				if (mBoss instanceof Mob bossMob) {
					bossMob.setTarget(null);
				}
				mCoweringTicks = 0;
			}
			++mCoweringTicks;
		}
	}

	public State getState() {
		return mState;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	@Override
	public boolean bypassSilence() {
		return true;
	}
}
