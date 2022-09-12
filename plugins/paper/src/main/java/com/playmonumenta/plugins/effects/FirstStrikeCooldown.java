package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;

public class FirstStrikeCooldown extends ZeroArgumentEffect {
	public static final String effectID = "FirstStrikeCooldown";

	public FirstStrikeCooldown(int duration) {
		super(duration, effectID);
	}

	public static FirstStrikeCooldown deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new FirstStrikeCooldown(duration);
	}

	@Override
	public String toString() {
		return String.format("FirstStrikeCooldown duration:%d", this.getDuration());
	}
}
