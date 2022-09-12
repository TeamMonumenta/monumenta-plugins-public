package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import org.bukkit.entity.Entity;

public class RecoilDisable extends Effect {
	public static final String effectID = "RecoilDisable";

	private final double mAmount;

	public RecoilDisable(int duration, int amount) {
		super(duration, effectID);
		mAmount = amount;
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();

		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("amount", mAmount);

		return object;
	}

	public static RecoilDisable deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		int amount = object.get("amount").getAsInt();

		return new RecoilDisable(duration, amount);
	}

	@Override
	public String toString() {
		return String.format("RecoilDisable duration:%d amount:%f", this.getDuration(), mAmount);
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (fourHertz) {
			if (entity.isOnGround()) {
				setDuration(0);
			}
		}
	}
}
