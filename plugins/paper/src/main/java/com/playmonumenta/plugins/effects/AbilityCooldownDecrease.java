package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AbilityCooldownDecrease extends SingleArgumentEffect {
	public static final String GENERIC_NAME = "AbilityCooldownDecrease";

	public AbilityCooldownDecrease(int duration, double amount) {
		super(duration, amount);
	}

	@Override
	public double getMagnitude() {
		return -1 * mAmount;
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(mAmount) + " Ability Cooldown Decrease";
	}

	@Override
	public String toString() {
		return String.format("AbilityCooldownDecrease duration:%d amount:%f", getDuration(), mAmount);
	}
}
