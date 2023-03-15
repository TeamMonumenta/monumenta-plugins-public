package com.playmonumenta.plugins.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Objects;
import org.bukkit.entity.Player;

public abstract class MultipleChargeAbility extends Ability implements AbilityWithChargesOrStacks {
	protected int mMaxCharges;

	protected int mCharges = 0;

	protected boolean mWasOnCooldown;
	ClassAbility mLinkedSpell;

	public MultipleChargeAbility(Plugin plugin, Player player, AbilityInfo<?> info) {
		super(plugin, player, info);
		mLinkedSpell = Objects.requireNonNull(info.getLinkedSpell());
	}

	// Call this when the ability is cast; returns whether a charge was consumed or not
	protected boolean consumeCharge() {
		if (mCharges > 0) {
			mCharges--;
			PlayerUtils.callAbilityCastEvent(mPlayer, mLinkedSpell);
			if (mMaxCharges > 1) {
				showChargesMessage();
			}
			ClientModHandler.updateAbility(mPlayer, this);
			AbilityManager.getManager().trackCharges(mPlayer, mLinkedSpell, mCharges);

			return true;
		}

		return false;
	}

	public boolean incrementCharge() {
		if (mCharges < mMaxCharges) {
			mCharges++;
			if (mMaxCharges > 1) {
				showChargesMessage();
			} else {
				showOffCooldownMessage();
			}
			ClientModHandler.updateAbility(mPlayer, this);
			AbilityManager.getManager().trackCharges(mPlayer, mLinkedSpell, mCharges);

			return true;
		}

		return false;
	}

	// This must be manually called if PeriodicTrigger is overridden by the superclass
	protected void manageChargeCooldowns() {
		boolean onCooldown = isOnCooldown();

		// If the skill is somehow on cooldown when charges are full, take it off cooldown
		if (mCharges == mMaxCharges && onCooldown) {
			mPlugin.mTimers.removeCooldown(mPlayer, mLinkedSpell);
		}

		boolean needsClientModUpdate = false;

		// Increment charges if last check was on cooldown, and now is off cooldown.
		if (mCharges < mMaxCharges && mWasOnCooldown && !onCooldown) {
			mCharges++;
			if (mMaxCharges > 1) {
				showChargesMessage();
			} else {
				showOffCooldownMessage();
			}
			needsClientModUpdate = true;
			AbilityManager.getManager().trackCharges(mPlayer, mLinkedSpell, mCharges);
		}

		// Put on cooldown if charges can still be gained
		if (mCharges < mMaxCharges && !onCooldown) {
			putOnCooldown();
			needsClientModUpdate = false; // putOnCooldown() already sends an update
		}

		if (needsClientModUpdate) {
			ClientModHandler.updateAbility(mPlayer, this);
		}

		mWasOnCooldown = onCooldown;
	}

	// Remove the call to AbilityCastEvent, which is done instead on charge consumption
	@Override
	public void putOnCooldown() {
		if (!isOnCooldown()) {
			mPlugin.mTimers.addCooldown(mPlayer, mLinkedSpell, getModifiedCooldown());
		}
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		manageChargeCooldowns();
	}

	@Override
	public int getCharges() {
		return mCharges;
	}

	@Override
	public int getMaxCharges() {
		return mMaxCharges;
	}

	public int getTrackedCharges() {
		if (mLinkedSpell != null) {
			return Math.min(AbilityManager.getManager().getTrackedCharges(mPlayer, mLinkedSpell), mMaxCharges);
		}
		return 0;
	}

}
