package com.playmonumenta.plugins.abilities;

public interface AbilityWithChargesOrStacks {

	/**
	 * @return the current number of charges or stacks of this ability
	 */
	int getCharges();

	/**
	 * @return the maximum number of charges or stacks this ability can have
	 */
	int getMaxCharges();

}
