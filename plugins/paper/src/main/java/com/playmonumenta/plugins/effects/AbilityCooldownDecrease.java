package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AbilityCooldownDecrease extends SingleArgumentEffect {
	public static final String GENERIC_NAME = "AbilityCooldownDecrease";
	public static final String effectID = "AbilityCooldownDecrease";

	public AbilityCooldownDecrease(int duration, double amount) {
		super(duration, amount, effectID);
	}

	@Override
	public double getMagnitude() {
		return -1 * mAmount;
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(mAmount) + " Ability Cooldown Decrease";
	}

	public static AbilityCooldownDecrease deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();

		return new AbilityCooldownDecrease(duration, amount);
	}

	@Override
	public String toString() {
		return String.format("AbilityCooldownDecrease duration:%d amount:%f", getDuration(), mAmount);
	}
}
