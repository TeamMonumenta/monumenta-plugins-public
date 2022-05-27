package com.playmonumenta.plugins.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import javax.annotation.Nullable;
import org.bukkit.entity.Player;

public abstract class MultipleChargeAbility extends Ability implements AbilityWithChargesOrStacks {

	protected int mMaxCharges;

	protected int mCharges = 0;

	protected boolean mWasOnCooldown;

	public MultipleChargeAbility(Plugin plugin, @Nullable Player player, String displayName) {
		super(plugin, player, displayName);
	}

	// Call this when the ability is cast; returns whether a charge was consumed or not
	protected boolean consumeCharge() {
		if (mPlayer != null && mCharges > 0) {
			mCharges--;
			PlayerUtils.callAbilityCastEvent(mPlayer, mInfo.mLinkedSpell);
			if (mMaxCharges > 1) {
				MessagingUtils.sendActionBarMessage(mPlayer, mInfo.mLinkedSpell.getName() + " Charges: " + mCharges);
			}
			ClientModHandler.updateAbility(mPlayer, this);
			AbilityManager.getManager().trackCharges(mPlayer, mInfo.mLinkedSpell, mCharges);

			return true;
		}

		return false;
	}

	protected boolean incrementCharge() {
		if (mPlayer != null && mCharges < mMaxCharges) {
			mCharges++;
			if (mMaxCharges > 1) {
				MessagingUtils.sendActionBarMessage(mPlayer, mInfo.mLinkedSpell.getName() + " Charges: " + mCharges);
			} else {
				MessagingUtils.sendActionBarMessage(mPlayer, mInfo.mLinkedSpell.getName() + " is now off cooldown!");
			}
			ClientModHandler.updateAbility(mPlayer, this);
			AbilityManager.getManager().trackCharges(mPlayer, mInfo.mLinkedSpell, mCharges);

			return true;
		}

		return false;
	}

	// This must be manually called if PeriodicTrigger is overridden by the superclass
	protected void manageChargeCooldowns() {
		if (mPlayer == null) {
			return;
		}
		boolean onCooldown = mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell);

		// If the skill is somehow on cooldown when charges are full, take it off cooldown
		if (mCharges == mMaxCharges && onCooldown) {
			mPlugin.mTimers.removeCooldown(mPlayer, mInfo.mLinkedSpell);
		}

		boolean needsClientModUpdate = false;

		// Increment charges if last check was on cooldown, and now is off cooldown.
		if (mCharges < mMaxCharges && mWasOnCooldown && !onCooldown) {
			mCharges++;
			if (mMaxCharges > 1) {
				MessagingUtils.sendActionBarMessage(mPlayer, mInfo.mLinkedSpell.getName() + " Charges: " + mCharges);
			} else {
				MessagingUtils.sendActionBarMessage(mPlayer, mInfo.mLinkedSpell.getName() + " is now off cooldown!");
			}
			needsClientModUpdate = true;
			AbilityManager.getManager().trackCharges(mPlayer, mInfo.mLinkedSpell, mCharges);
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
		if (mPlayer != null && !mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
			mPlugin.mTimers.addCooldown(mPlayer, mInfo.mLinkedSpell, getModifiedCooldown());
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
		if (mPlayer != null && mInfo.mLinkedSpell != null) {
			return Math.min(AbilityManager.getManager().getTrackedCharges(mPlayer, mInfo.mLinkedSpell), mMaxCharges);
		}
		return 0;
	}

}
