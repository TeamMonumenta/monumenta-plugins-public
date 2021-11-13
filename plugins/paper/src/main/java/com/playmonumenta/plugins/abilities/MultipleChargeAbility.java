package com.playmonumenta.plugins.abilities;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;

public abstract class MultipleChargeAbility extends Ability implements AbilityWithChargesOrStacks {

	protected int mMaxCharges;

	protected int mCharges;
	protected boolean mWasOnCooldown;

	public MultipleChargeAbility(Plugin plugin, Player player,
			String displayName, int maxCharges1, int maxCharges2) {
		super(plugin, player, displayName);

		// getAbilityScore() doesn't work until we set the mInfo.mScoreboard in the child class
		new BukkitRunnable() {
			@Override
			public void run() {
				mMaxCharges = getAbilityScore() == 1 ? maxCharges1 : maxCharges2;
				mCharges = 0;
			}
		}.runTaskLater(mPlugin, 1);
	}

	// Call this when the ability is cast; returns whether a charge was consumed or not
	protected boolean consumeCharge() {
		if (mCharges > 0) {
			mCharges--;
			PlayerUtils.callAbilityCastEvent(mPlayer, mInfo.mLinkedSpell);
			MessagingUtils.sendActionBarMessage(mPlayer, mInfo.mLinkedSpell.getName() + " Charges: " + mCharges);
			ClientModHandler.updateAbility(mPlayer, this);

			return true;
		}

		return false;
	}

	protected boolean incrementCharge() {
		if (mCharges < mMaxCharges) {
			mCharges++;
			MessagingUtils.sendActionBarMessage(mPlayer, mInfo.mLinkedSpell.getName() + " Charges: " + mCharges);
			ClientModHandler.updateAbility(mPlayer, this);

			return true;
		}

		return false;
	}

	// This must be manually called if PeriodicTrigger is overridden by the superclass
	protected void manageChargeCooldowns() {
		boolean onCooldown = mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell);

		// If the skill is somehow on cooldown when charges are full, take it off cooldown
		if (mCharges == mMaxCharges && onCooldown) {
			mPlugin.mTimers.removeCooldown(mPlayer, mInfo.mLinkedSpell);
		}

		boolean needsClientModUpdate = false;

		// Increment charges if last check was on cooldown, and now is off cooldown.
		if (mCharges < mMaxCharges && mWasOnCooldown && !onCooldown) {
			mCharges++;
			MessagingUtils.sendActionBarMessage(mPlayer, mInfo.mLinkedSpell.getName() + " Charges: " + mCharges);
			needsClientModUpdate = true;
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
		if (!mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
			mPlugin.mTimers.addCooldown(mPlayer, mInfo.mLinkedSpell, mInfo.mCooldown);
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

}
