package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

public class CustomTimerEffect extends Effect {
	public static final String effectID = "customtimer";
	private String mCustomDisplay;
	private int mAmount;

	public CustomTimerEffect(int duration, String customDisplay) {
		super(duration, effectID);
		mCustomDisplay = customDisplay;
		mAmount = 0;
	}

	public CustomTimerEffect(int duration, int magntitude, String customDisplay) {
		super(duration, effectID);
		mAmount = magntitude;
		mCustomDisplay = customDisplay;
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return mCustomDisplay;
	}


	@Override
	public String toString() {
		return String.format("Custom Timer %d", this.getDuration());
	}


	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		return object;
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}
}
