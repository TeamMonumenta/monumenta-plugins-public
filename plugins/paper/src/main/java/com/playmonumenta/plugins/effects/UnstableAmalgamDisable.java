package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import org.bukkit.entity.Entity;

public class UnstableAmalgamDisable extends Effect {
	public static final String effectID = "UnstableAmalgamDisable";


	public UnstableAmalgamDisable(int duration) {
		super(duration, effectID);
	}

	@Override
	public double getMagnitude() {
		return 1;
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();

		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);

		return object;
	}

	public static UnstableAmalgamDisable deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new UnstableAmalgamDisable(duration);
	}

	@Override
	public String toString() {
		return String.format("RecoilDisable duration:%d", this.getDuration());
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
