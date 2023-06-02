package com.playmonumenta.plugins.abilities;

/**
 * Interface representing an ability which has a duration
 * Should be implemented if the ability uses a BukkitRunnable
 * Should not if the ability uses an effect.
 */
public interface AbilityWithDuration {

	/**
	 * Returns the base duration of the skill
	 */
	int getInitialAbilityDuration();

	/**
	 * Returns the time left before the skill ends
	 */
	int getRemainingAbilityDuration();
}
