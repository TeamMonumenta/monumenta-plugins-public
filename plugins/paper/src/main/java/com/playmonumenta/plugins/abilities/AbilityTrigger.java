package com.playmonumenta.plugins.abilities;

/**
 * Not much really. It's to decide whether the ability
 * initiates manually via Right or Left Click. May remove
 * this and just have it activate on PlayerInteractEvent.
 * Just a rough draft really.
 * @author FirelordWeaponry (Fire)
 *
 */
public enum AbilityTrigger {
	ALL,
	RIGHT_CLICK,
	RIGHT_CLICK_AIR,
	LEFT_CLICK,
	LEFT_CLICK_AIR
}
