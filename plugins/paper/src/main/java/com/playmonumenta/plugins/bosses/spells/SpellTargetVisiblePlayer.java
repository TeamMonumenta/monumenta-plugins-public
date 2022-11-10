package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

/*
 * This spell is designed to run as a passive. Here are the rules:
 * - Do not change mob target more than once every 3s
 * - If mob has line of sight to player within range, set cooldown to change target back to 3s
 * - If mob does not have line of sight to player, tick down cooldown
 * - If cooldown elapsed, mob targets the closest player with line of sight
 * - If no players have line of sight, the mob continues to target the last seen player, even through walls
 * - If mob has a new player target (potentially a taunt), set mLastTarget to that new target instead.
 */
public class SpellTargetVisiblePlayer extends Spell {
	private static final int PERIOD = 5;

	private final Mob mBoss;
	private final int mDetectionRange;
	private final int mCooldown;
	private final int mForgetTicks;

	private int mCooldownRemaining;
	private int mTicksSinceLastSeen;
	private @Nullable Player mLastTarget;

	public SpellTargetVisiblePlayer(Mob launcher, int detectionRange, int targetSwitchCooldown, int forgetTicks) {
		mBoss = launcher;
		mDetectionRange = detectionRange;
		mCooldown = targetSwitchCooldown;
		mForgetTicks = forgetTicks;

		mCooldownRemaining = 0;
		mLastTarget = null;
	}

	@Override
	public void cancel() {
		mBoss.setTarget(null);
	}

	@Override
	public void run() {
		mCooldownRemaining -= PERIOD;
		mTicksSinceLastSeen += PERIOD;

		if (EntityUtils.isStunned(mBoss)) {
			return;
		}

		// Forget about this target if they die, leave the game, switch to spectator, or are invisible
		if (mLastTarget != null) {
			if (!mLastTarget.isValid() || mLastTarget.getGameMode() == GameMode.SPECTATOR || AbilityUtils.isStealthed(mLastTarget)) {
				mLastTarget = null;
				mBoss.setTarget(null);
				mCooldownRemaining = 0;
			}
		}

		// Check if current target is still visible
		if (mLastTarget != null && LocationUtils.hasLineOfSight(mBoss, mLastTarget)) {
			mTicksSinceLastSeen = 0;

			// If target has changed and is a player, set mLastTarget to that new target (to allow taunts)
			// Otherwise, if it is NOT a player, change it back to mLastTarget.
			if (mBoss.getTarget() != mLastTarget) {
				LivingEntity target = mBoss.getTarget();
				if (target instanceof Player player) {
					mLastTarget = player;
				} else {
					mBoss.setTarget(mLastTarget);
				}
			}

			// Refresh the cooldown and end here
			mCooldownRemaining = mCooldown;
		} else {
			// No line of sight.
			if (mCooldownRemaining > 0) {
				// Continue targeting last target even though they are not visible.
				// If target is a different player, set that new player as the new target.
				// Otherwise, continue chasing mLastTarget.
				if (mLastTarget != null && mBoss.getTarget() != mLastTarget) {
					LivingEntity target = mBoss.getTarget();
					if (target instanceof Player player) {
						mLastTarget = player;
					} else {
						mBoss.setTarget(mLastTarget);
					}
				}
			} else {
				// Potentially find a new target
				Location bossLoc = mBoss.getEyeLocation();
				List<Player> potentialTargets = PlayerUtils.playersInRange(bossLoc, mDetectionRange, false);
				Collections.sort(potentialTargets, (a, b) -> Double.compare(a.getLocation().distance(bossLoc), b.getLocation().distance(bossLoc)));

				for (Player player : potentialTargets) {
					if (LocationUtils.hasLineOfSight(mBoss, player)) {
						mLastTarget = player;
						mBoss.setTarget(player);
						mCooldownRemaining = mCooldown;
						mTicksSinceLastSeen = 0;
						break;
					}
				}

				if (mTicksSinceLastSeen > mForgetTicks) {
					// Haven't seen any players in a while - resume wandering
					mBoss.setTarget(null);
					mLastTarget = null;
					mCooldownRemaining = 0;
				}
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return PERIOD;
	}
}
