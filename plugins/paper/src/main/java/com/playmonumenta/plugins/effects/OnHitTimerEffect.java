package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;

public class OnHitTimerEffect extends Effect {
	protected final double mAmount;
	public static final String effectID = "OnHitTimerEffect";

	public OnHitTimerEffect(int duration, int amount) {
		super(duration, effectID);
		mAmount = amount;
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	public static OnHitTimerEffect deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		int amount = object.get("amount").getAsInt();

		return new OnHitTimerEffect(duration, amount);
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
	public String toString() {
		return String.format("OnHitTimerEffect duration:%d  amount:%s", this.getDuration(), this.getMagnitude());
	}
}
