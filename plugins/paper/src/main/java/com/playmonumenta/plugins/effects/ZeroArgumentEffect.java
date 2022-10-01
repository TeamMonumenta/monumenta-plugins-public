package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;

public abstract class ZeroArgumentEffect extends Effect {
	public ZeroArgumentEffect(int duration, String effectID) {
		super(duration, effectID);
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);

		return object;
	}
}
