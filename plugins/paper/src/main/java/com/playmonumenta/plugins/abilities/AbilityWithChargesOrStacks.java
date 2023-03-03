package com.playmonumenta.plugins.abilities;

public interface AbilityWithChargesOrStacks {
	enum ChargeType {
		CHARGES("Charges"),
		STACKS("Stacks");

		final String mDisplayName;

		ChargeType(String displayName) {
			mDisplayName = displayName;
		}

		@Override
		public String toString() {
			return mDisplayName;
		}
	}

	/**
	 * The text to use in ABILITY + Charges/Stacks/Other: #
	 */
	default ChargeType getChargeType() {
		return ChargeType.CHARGES;
	}

	/**
	 * The current number of charges or stacks of this ability
	 */
	int getCharges();

	/**
	 * The maximum number of charges or stacks this ability can have
	 */
	int getMaxCharges();

}
