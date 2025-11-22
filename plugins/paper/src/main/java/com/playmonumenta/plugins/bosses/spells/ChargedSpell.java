package com.playmonumenta.plugins.bosses.spells;

public class ChargedSpell extends Spell {
	private final int mChargeInterval;
	private final int mMaxCharges;
	private final int mCooldown;
	private int mCharges = 0;

	public ChargedSpell(int maxCharges, int chargeInterval, int cooldown) {
		mChargeInterval = chargeInterval;
		mMaxCharges = maxCharges;
		mCooldown = cooldown;
	}

	@Override
	public void run() {
		consumeCharge();
		launch();
	}

	private void consumeCharge() {
		if (mCharges > 0) {
			mCharges--;
		} else {
			mCharges = mMaxCharges - 1;
		}
	}

	// Override to run this every time the spell runs
	protected void launch() {

	}

	@Override
	public int cooldownTicks() {
		return mCharges > 0 ? mChargeInterval : mCooldown;
	}
}
