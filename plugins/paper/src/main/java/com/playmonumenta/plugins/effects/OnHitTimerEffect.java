package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;

public class OnHitTimerEffect extends ZeroArgumentEffect {
	public static final String effectID = "OnHitTimerEffect";

	public OnHitTimerEffect(int duration) {
		super(duration, effectID);
	}

	public static OnHitTimerEffect deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new OnHitTimerEffect(duration);
	}

	@Override
	public String toString() {
		return String.format("OnHitTimerEffect duration:%d", this.getDuration());
	}
}
