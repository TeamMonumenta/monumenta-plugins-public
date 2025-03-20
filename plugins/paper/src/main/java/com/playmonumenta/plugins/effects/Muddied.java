package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;

public class Muddied extends SingleArgumentEffect {
	public static final String effectID = "Muddied";

	private int mMudBlocksRemaining;

	public Muddied(int duration, int amount) {
		super(duration, amount, effectID);
		mMudBlocksRemaining = amount;
	}

	public static Muddied deserialize(JsonObject object) {
		int duration = object.get("duration").getAsInt();
		int amount = object.get("amount").getAsInt();

		return new Muddied(duration, amount);
	}

	@Override
	public String toString() {
		return String.format("Muddied duration:%d", getDuration());
	}

	public int getMudBlocksRemaining() {
		return mMudBlocksRemaining;
	}

	public void mudBlockBroken() {
		mMudBlocksRemaining = Math.max(0, mMudBlocksRemaining - 1);
	}
}
