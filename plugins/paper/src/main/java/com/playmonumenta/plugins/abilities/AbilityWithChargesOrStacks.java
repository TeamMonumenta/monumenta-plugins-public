package com.playmonumenta.plugins.abilities;

public interface AbilityWithChargesOrStacks {

	/**
	 * The current number of charges or stacks of this ability
	 */
	int getCharges();

	/**
	 * The maximum number of charges or stacks this ability can have
	 */
	int getMaxCharges();

}
