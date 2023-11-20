package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;

public class FractalCooldown extends ZeroArgumentEffect {
	public static final String effectID = "FractalCooldown";

	public FractalCooldown(int duration) {
		super(duration, effectID);
	}

	public static FractalCooldown deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new FractalCooldown(duration);
	}

	@Override
	public String toString() {
		return String.format("FractalCooldown duration:%d", this.getDuration());
	}
}
