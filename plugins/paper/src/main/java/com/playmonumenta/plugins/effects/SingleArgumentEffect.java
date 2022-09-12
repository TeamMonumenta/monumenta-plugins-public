package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;

public abstract class SingleArgumentEffect extends Effect {
	protected final double mAmount;

	public SingleArgumentEffect(int duration, double amount, String effectID) {
		super(duration, effectID);
		mAmount = amount;
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();

		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("amount", mAmount);

		return object;
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}
}
