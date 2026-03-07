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

	ClassAbility mLinkedSpell;

	public MultipleChargeAbility(Plugin plugin, Player player, AbilityInfo<?> info) {
		super(plugin, player, info);
		mLinkedSpell = Objects.requireNonNull(info.getLinkedSpell());
	}

	// Call this when the ability is cast; returns whether a charge was consumed or not
	protected boolean consumeCharge() {
		if (mCharges > 0) {
			mCharges--;
			putOnCooldown();
			if (mMaxCharges > 1) {
				showChargesMessage();
			}

			return true;
		}

		return false;
	}

	// Tries to consume all charges; returns the number of charges consumed.
	protected int consumeAllCharge() {
		if (mCharges > 0) {
			int charges = mCharges;
			mCharges = 0;
			for (int i = 0; i < charges; i++) {
				putOnCooldown(false);
			}
			PlayerUtils.callAbilityCastEvent(mPlayer, this, mLinkedSpell, 0);
			ClientModHandler.updateAbility(mPlayer, this);
			return charges;
		}

		return 0;
	}

	// This must be manually called if PeriodicTrigger is overridden by the superclass
	protected void manageChargeCooldowns() {
		int currentCharges = mCharges;
		updateCharges();
		if (mCharges != currentCharges && mMaxCharges > 1) {
			showChargesMessage();
			ClientModHandler.updateAbility(mPlayer, this);
		}

		// If the skill is somehow on cooldown when charges are full, take it off cooldown
		if (mCharges == mMaxCharges && isOnCooldown()) {
			mPlugin.mTimers.removeCooldown(mPlayer, mLinkedSpell);
		}
	}

	public void updateCharges() {
		mCharges = getChargesOffCooldown();
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

	private int getChargesOnCooldown() {
		return mPlugin.mTimers.getCooldownList(mPlayer.getUniqueId(), mLinkedSpell).size();
	}

	public int getChargesOffCooldown() {
		return Math.max(mMaxCharges - getChargesOnCooldown(), 0);
	}

}
