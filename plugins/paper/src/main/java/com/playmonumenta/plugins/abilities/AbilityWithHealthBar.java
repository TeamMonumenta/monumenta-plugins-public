package com.playmonumenta.plugins.abilities;

/**
 * Interface representing an ability which has a health bar
 */
public interface AbilityWithHealthBar {

	/**
	 * Returns the base health of the skill
	 */
	double getInitialAbilityHealth();

	/**
	 * Returns the health left before the skill ends/dies
	 */
	double getRemainingAbilityHealth();
}
