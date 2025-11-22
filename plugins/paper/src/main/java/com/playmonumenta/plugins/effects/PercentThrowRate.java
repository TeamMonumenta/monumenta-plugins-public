package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public class PercentThrowRate extends Effect {
	public static final String GENERIC_NAME = "PercentThrowRate";
	public static final String effectID = "PercentThrowRate";
	private final double mAmount;

	public PercentThrowRate(final int duration, final double amount) {
		super(duration, effectID);
		mAmount = amount;
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	@Override
	public boolean isBuff() {
		return mAmount > 0;
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(mAmount).append(Component.text(" " + getDisplayedName()));
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "Throw Rate";
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("amount", mAmount);

		return object;
	}

	public static PercentAttackSpeed deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();
		String modName = object.get("modifierName").getAsString();

		return new PercentAttackSpeed(duration, amount, modName);
	}

	@Override
	public String toString() {
		return String.format("PercentThrowRate duration:%d amount:%f", this.getDuration(), mAmount);
	}
}
