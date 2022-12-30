package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.StringUtils;
import org.jetbrains.annotations.Nullable;

public class AbilityCooldownIncrease extends SingleArgumentEffect {
	public static final String GENERIC_NAME = "AbilityCooldownIncrease";
	public static final String effectID = "AbilityCooldownIncrease";

	public AbilityCooldownIncrease(int duration, double amount) {
		super(duration, amount, effectID);
	}

	public static AbilityCooldownIncrease deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();

		return new AbilityCooldownIncrease(duration, amount);
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
