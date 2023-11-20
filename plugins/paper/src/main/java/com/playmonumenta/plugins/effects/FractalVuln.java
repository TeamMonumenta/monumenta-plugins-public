package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;

public class FractalVuln extends ZeroArgumentEffect {
	public static final String effectID = "FractalVuln";

	public FractalVuln(int duration) {
		super(duration, effectID);
	}

	public static FractalVuln deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new FractalVuln(duration);
	}

	@Override
	public String toString() {
		return String.format("FractalVuln duration:%d", this.getDuration());
	}
}
