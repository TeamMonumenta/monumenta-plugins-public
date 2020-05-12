package com.playmonumenta.plugins.cooking;

import java.util.ArrayList;

public enum CookingEffectsEnum {

	// positive effects
	ABSORPTION_POTENCY(Kind.POTENCY, CookingConsts.POSITIVE_EFFECT_COLOR + "Absorption %s "),
	CONDUIT_POWER_POTENCY(Kind.POTENCY, CookingConsts.POSITIVE_EFFECT_COLOR + "Conduit Power %s "),
	RESISTANCE_POTENCY(Kind.POTENCY, CookingConsts.POSITIVE_EFFECT_COLOR + "Resistance %s "),
	DOLPHINS_GRACE_POTENCY(Kind.POTENCY, CookingConsts.POSITIVE_EFFECT_COLOR + "D.Grace %s "),
	REGENERATION_POTENCY(Kind.POTENCY, CookingConsts.POSITIVE_EFFECT_COLOR + "Regeneration %s "),
	STRENGTH_POTENCY(Kind.POTENCY, CookingConsts.POSITIVE_EFFECT_COLOR + "Strength %s "),
	HASTE_POTENCY(Kind.POTENCY, CookingConsts.POSITIVE_EFFECT_COLOR + "Haste %s "),
	FIRE_RESISTANCE_POTENCY(Kind.POTENCY, CookingConsts.POSITIVE_EFFECT_COLOR + "Fire Resistance %s "),
	GLOWING_POTENCY(Kind.POTENCY, CookingConsts.POSITIVE_EFFECT_COLOR + "Glowing %s "),
	HEALTH_BOOST_POTENCY(Kind.POTENCY, CookingConsts.POSITIVE_EFFECT_COLOR + "Health boost %s "),
	INVISIBILITY_POTENCY(Kind.POTENCY, CookingConsts.POSITIVE_EFFECT_COLOR + "Invisibility %s "),
	JUMP_POTENCY(Kind.POTENCY, CookingConsts.POSITIVE_EFFECT_COLOR + "Jump boost %s "),
	NIGHT_VISION_POTENCY(Kind.POTENCY, CookingConsts.POSITIVE_EFFECT_COLOR + "Night Vision %s "),
	SATURATION_POTENCY(Kind.POTENCY, CookingConsts.POSITIVE_EFFECT_COLOR + "Saturation effect %s "),
	SLOW_FALLING_POTENCY(Kind.POTENCY, CookingConsts.POSITIVE_EFFECT_COLOR + "Slow Falling %s "),
	SPEED_POTENCY(Kind.POTENCY, CookingConsts.POSITIVE_EFFECT_COLOR + "Speed %s "),
	WATER_BREATHING_POTENCY(Kind.POTENCY, CookingConsts.POSITIVE_EFFECT_COLOR + "Water Breathing %s "),

	// negative effects
	BLINDNESS_POTENCY(Kind.POTENCY, CookingConsts.NEGATIVE_EFFECT_COLOR + "Blindness %s"),
	HUNGER_POTENCY(Kind.POTENCY, CookingConsts.NEGATIVE_EFFECT_COLOR + "Hunger effect %s "),
	NAUSEA_POTENCY(Kind.POTENCY, CookingConsts.NEGATIVE_EFFECT_COLOR + "Nausea %s "),
	POISON_POTENCY(Kind.POTENCY, CookingConsts.NEGATIVE_EFFECT_COLOR + "Poison %s "),
	LEVITATION_POTENCY(Kind.POTENCY, CookingConsts.NEGATIVE_EFFECT_COLOR + "Levitation %s "),
	SLOWNESS_POTENCY(Kind.POTENCY, CookingConsts.NEGATIVE_EFFECT_COLOR + "Slowness %s "),
	MINING_FATIGUE_POTENCY(Kind.POTENCY, CookingConsts.NEGATIVE_EFFECT_COLOR + "Mining Fatigue %s "),
	WEAKNESS_POTENCY(Kind.POTENCY, CookingConsts.NEGATIVE_EFFECT_COLOR + "Weakness %s "),
	WITHER_POTENCY(Kind.POTENCY, CookingConsts.NEGATIVE_EFFECT_COLOR + "Wither %s "),

	// custom positive effects

	// custom negative effects
	FIRE_POTENCY(Kind.POTENCY, CookingConsts.NEGATIVE_EFFECT_COLOR + "On Fire %s "),

	// custom active effects

	// durations
	ABSORPTION_DURATION(Kind.DURATION, "(%s)"),
	CONDUIT_DURATION(Kind.DURATION, "(%s)"),
	RESISTANCE_DURATION(Kind.DURATION, "(%s)"),
	DOLPHIN_DURATION(Kind.DURATION, "(%s)"),
	REGENERATION_DURATION(Kind.DURATION, "(%s)"),
	STRENGTH_DURATION(Kind.DURATION, "(%s)"),
	HASTE_DURATION(Kind.DURATION, "(%s)"),
	FIRE_RESISTANCE_DURATION(Kind.DURATION, "(%s)"),
	GLOWING_DURATION(Kind.DURATION, "(%s)"),
	HEALTH_BOOST_DURATION(Kind.DURATION, "(%s)"),
	INVISIBILITY_DURATION(Kind.DURATION, "(%s)"),
	JUMP_DURATION(Kind.DURATION, "(%s)"),
	NIGHT_VISION_DURATION(Kind.DURATION, "(%s)"),
	SATURATION_DURATION(Kind.DURATION, "(%s)"),
	SLOW_FALLING_DURATION(Kind.DURATION, "(%s)"),
	SPEED_DURATION(Kind.DURATION, "(%s)"),
	WATER_BREATHING_DURATION(Kind.DURATION, "(%s)"),

	BLINDNESS_DURATION(Kind.DURATION, "(%s)"),
	HUNGER_DURATION(Kind.DURATION, "(%s)"),
	NAUSEA_DURATION(Kind.DURATION, "(%s)"),
	POISON_DURATION(Kind.DURATION, "(%s)"),
	SLOWNESS_DURATION(Kind.DURATION, "(%s)"),
	MINING_FATIGUE_DURATION(Kind.DURATION, "(%s)"),
	WEAKNESS_DURATION(Kind.DURATION, "(%s)"),
	WITHER_DURATION(Kind.DURATION, "(%s)"),

	FIRE_DURATION(Kind.DURATION, "(%s)"),

	// food values
	HUNGER(Kind.FOOD, "%+d Hunger"),
	SATURATION(Kind.FOOD, "%+d Saturation"),
	HEALTH(Kind.FOOD, "%+d Health"),

	// modifiers
	DURATION_MULT(Kind.OTHER, "Effects Duration: %+d%%"),
	DURATION_ADD(Kind.OTHER, "Effects Duration: %+d"),
	POTENCY_ADD(Kind.OTHER, "Effects Potency: %+d"),
	FOOD_VALUES_MULT(Kind.OTHER, "Food Values: %+d%%"),
	FOOD_VALUES_ADD(Kind.OTHER, "Food Values: %+d"),
	ITEM_AMOUNT_ADD(Kind.OTHER,	"Output Amount: %+d"),
	ITEM_AMOUNT_MULT(Kind.OTHER,	"Output Amount: %+d%%");

	enum Kind {
		POTENCY,
		DURATION,
		FOOD,
		OTHER,
	}

	String mReadableStr;
	Kind mKind;

	CookingEffectsEnum(Kind k, String s) {
		this.mKind = k;
		this.mReadableStr = s;
	}

	Kind getKind() {
		return this.mKind;
	}

	String getReadableStr() {
		return this.mReadableStr;
	}

	static String[] valuesAsString() {
		ArrayList<String> out = new ArrayList<>();
		for (CookingEffectsEnum e : CookingEffectsEnum.values()) {
			out.add(e.toString());
		}
		return out.toArray(new String[0]);
	}

	static CookingEffectsEnum[] potencyValues() {
		CookingEffectsEnum[] all = CookingEffectsEnum.values();
		ArrayList<CookingEffectsEnum> out = new ArrayList<>();
		for (CookingEffectsEnum e : all) {
			if (e.mKind == Kind.POTENCY) {
				out.add(e);
			}
		}
		return out.toArray(new CookingEffectsEnum[0]);
	}
}
