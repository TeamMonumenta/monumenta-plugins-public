package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;


public class SpellBaseSeekingAoE extends Spell {

	@FunctionalInterface
	public interface ChargeAction {
		/**
		 * Function called every two ticks during the charge up
		 *
		 * @param location Current location of the spell
		 * @param ticks Amount of ticks passed since the spell started charging up
		 */
		void run(Location location, int ticks);
	}

	@FunctionalInterface
	public interface CastAction {
		/**
		 * Function called when the charge up is done and the spell is cast
		 *
		 * @param location Current location of the spell
		 * @param spellCount Which spell it currently casting for whenever it mCount is more than 1
		 */
		void run(Location location, int spellCount);
	}

	@FunctionalInterface
	public interface OutburstAction {
		/**
		 * Function called when the spell is triggered after the cast delay is up.
		 *
		 * @param location Current location of the spell
		 * @param spellCount Which spell it currently casting for whenever it mCount is more than 1
		 */
		void run(Location location, int spellCount);
	}

	@FunctionalInterface
	public interface HitAction {
		/**
		 * Function called on hit for each hit player
		 *
		 * @param player The hit player
		 */
		void run(Player player);
	}

	protected final Plugin mPlugin;
	protected final LivingEntity mCaster;
	protected final int mDetection;
	protected final int mRange;
	protected final int mChargeUp;
	protected final int mDelay;
	protected final int mCooldown;
	protected final int mRadius;
	protected final int mCount;
	protected final int mIncrementDelay;
	protected final boolean mCanMoveWhileCharging;
	protected final boolean mCanMoveWhileCasting;
	protected final boolean mSingleTarget;
	protected final boolean mCancelOutsideRange;
	protected final boolean mKeepGrounded;
	protected final @Nullable Predicate<Player> mPlayerFilter;
	protected final ChargeAction mChargeAction;
	protected final CastAction mCastAction;
	protected final OutburstAction mOutburstAction;
	protected final HitAction mHitAction;

	/**
	 *
	 * @param plugin Plugin
	 * @param caster The boss casting the spell
	 * @param detection Range in which a player has to be in order for the spell to be charged and used
	 * @param range Maximum range the spell can be cast at
	 * @param chargeUp Time it takes for the spell to charge, while the spell is charging it follows the player
	 * @param delay Time it takes for the spell to trigger after it has finished charging and become stationary
	 * @param cooldown Cooldown
	 * @param radius Radius
	 * @param count Amount of spells per player
	 * @param incrementDelay Time between each consecutive spell when count is above 1
	 * @param canMoveWhileCharging If the boss can move while in the charge up phase
	 * @param canMoveWhileCasting If the boss can move in the period between the charge up is finished and the spell is triggered
	 * @param singleTarget If the boss can target more than 1 player at once
	 * @param cancelOutsideRange If the spell should be canceled outside max range.
	 *                           If false the spell will be cast at max range in the direction of the player
	 * @param keepGrounded If true the spell will always be cast at ground level
	 * @param chargeAction Action ran every tick while the aoe is seeking the player
	 * @param castAction Action ran when the placement of the spell is finalized
	 * @param outburstAction Action ran when the spell activates
	 * @param hitAction Action ran for each hit target
	 */
	public SpellBaseSeekingAoE(Plugin plugin, LivingEntity caster, int detection, int range, int chargeUp, int delay,
							   int cooldown, int radius, int count, int incrementDelay, boolean canMoveWhileCharging, boolean canMoveWhileCasting,
							   boolean singleTarget, boolean cancelOutsideRange, boolean keepGrounded, @Nullable Predicate<Player> playerFilter,
							   ChargeAction chargeAction, CastAction castAction, OutburstAction outburstAction, HitAction hitAction) {
		mPlugin = plugin;
		mCaster = caster;
		mDetection = detection;
		mRange = range;
		mChargeUp = chargeUp;
		mDelay = delay;
		mCooldown = cooldown;
		mRadius = radius;
		mCount = count;
		mIncrementDelay = incrementDelay;
		mCanMoveWhileCharging = canMoveWhileCharging;
		mCanMoveWhileCasting = canMoveWhileCasting;
		mSingleTarget = singleTarget;
		mCancelOutsideRange = cancelOutsideRange;
		mKeepGrounded = keepGrounded;
		mPlayerFilter = playerFilter;
		mChargeAction = chargeAction;
		mCastAction = castAction;
		mOutburstAction = outburstAction;
		mHitAction = hitAction;
	}

	@Override
	public void run() {

		List<Player> targets = new ArrayList<>();

		for (Player player : PlayerUtils.playersInRange(mCaster.getLocation(), mDetection, false)) {

			if (mPlayerFilter == null || mPlayerFilter.test(player)) {
				targets.add(player);

				if (mSingleTarget) {
					break;
				}
			}
		}

		if (!mCanMoveWhileCharging) {
			EntityUtils.selfRoot(mCaster, mChargeUp);
		}

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;

				if (EntityUtils.shouldCancelSpells(mCaster) || targets.isEmpty()) {
					this.cancel();
					if (!mCanMoveWhileCharging) {
						EntityUtils.cancelSelfRoot(mCaster);
					}
					return;
				}

				for (Player target : new ArrayList<>(targets)) {

					if (target.getLocation().distanceSquared(mCaster.getLocation()) > mRange * mRange && mCancelOutsideRange) {
						targets.remove(target);
						continue;
					}

					mChargeAction.run(getSpellLocation(target), mTicks);

					if (mTicks >= mChargeUp) {
						this.cancel();

						cast(target);
					}
				}


			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void cast(Player target) {

		if (!mCanMoveWhileCasting) {
			EntityUtils.selfRoot(mCaster, (mCount - 1) * mIncrementDelay + mDelay);
		}

		new BukkitRunnable() {
			int mSpellCounter = 0;

			@Override
			public void run() {
				mSpellCounter++;

				if (EntityUtils.shouldCancelSpells(mCaster) || mSpellCounter > mCount) {
					this.cancel();
					return;
				}

				if (target.getLocation().distanceSquared(mCaster.getLocation()) > mRange * mRange && mCancelOutsideRange) {
					this.cancel();
					EntityUtils.cancelSelfRoot(mCaster);
					return;
				}

				Location castLoc = getSpellLocation(target);
				mCastAction.run(castLoc, mSpellCounter);

				new BukkitRunnable() {

					@Override
					public void run() {

						mOutburstAction.run(castLoc, mSpellCounter);
						for (Player player : PlayerUtils.playersInRange(castLoc, mRadius, true)) {
							mHitAction.run(player);
						}
					}

				}.runTaskLater(mPlugin, mDelay);

			}

		}.runTaskTimer(mPlugin, 0, mIncrementDelay);
	}

	private Location getSpellLocation(Player target) {

		Location loc = target.getLocation();

		if (loc.distanceSquared(mCaster.getLocation()) > mRange * mRange && !mCancelOutsideRange) {
			loc = mCaster.getLocation();
			loc.add(LocationUtils.getDirectionTo(target.getLocation(), mCaster.getLocation()).multiply(mRange));
		}

		if (mKeepGrounded) {
			loc = LocationUtils.fallToGround(loc, Math.max(mCaster.getLocation().getY() - mRange, 0));
		}

		return loc;
	}


	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
