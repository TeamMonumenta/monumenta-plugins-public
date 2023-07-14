package com.playmonumenta.plugins.effects;

public class GearChanged extends ZeroArgumentEffect {
	public static final String effectID = "GearChanged";
	public static final int DURATION = 20 * 15;

	public GearChanged(int duration) {
		super(duration, effectID);
	}

	@Override
	public String toString() {
		return String.format("GearChanged duration:%d", this.getDuration());
	}
}
