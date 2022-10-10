package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

public class HealingSickness extends Effect {
	public static final String effectID = "HealingSickness";

	private final double mAmount;
	private final String mModifierName;

	public HealingSickness(int duration, double amount, String modifierName) {
		super(duration, effectID);
		mAmount = amount;
		mModifierName = modifierName;
		mDuration = duration;
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	@Override
	public boolean isDebuff() {
		return mAmount < 0;
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("amount", mAmount);
		object.addProperty("modifierName", mModifierName);

		return object;
	}

	public static HealingSickness deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();
		String modName = object.get("modifierName").getAsString();

		return new HealingSickness(duration, amount, modName);
	}

	@Override
	public boolean isBuff() {
		return false;
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return StringUtils.to2DP(mAmount * 100) + "% Healing Sickness";
	}

	@Override
	public String toString() {
		return String.format("Healing Sickness duration:%d modifier:%s amount:%f", this.getDuration(), mModifierName, mAmount);
	}
}
