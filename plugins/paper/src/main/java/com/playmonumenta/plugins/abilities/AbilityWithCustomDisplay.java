package com.playmonumenta.plugins.abilities;


import net.kyori.adventure.text.Component;

/**
 * Interface representing an ability which has a custom display
 */
public interface AbilityWithCustomDisplay {

	/**
	 * Returns the display component of the skill
	 */
	Component customDisplayComponent();
}
