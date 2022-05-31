package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AbilityCooldownIncrease extends SingleArgumentEffect {
	public static final String GENERIC_NAME = "AbilityCooldownIncrease";

	public AbilityCooldownIncrease(int duration, double amount) {
		super(duration, amount);
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(mAmount) + " Ability Cooldown Increase";
	}

	@Override
	public String toString() {
		return String.format("AbilityCooldownIncrease duration:%d amount:%f", getDuration(), mAmount);
	}
}
