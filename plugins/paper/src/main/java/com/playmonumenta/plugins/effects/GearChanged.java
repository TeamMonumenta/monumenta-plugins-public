package com.playmonumenta.plugins.effects;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

public class GearChanged extends ZeroArgumentEffect {
	public static final String effectID = "GearChanged";
	public static final int DURATION = TICKS_PER_SECOND * 15;

	public GearChanged(int duration) {
		super(duration, effectID);
	}

	@Override
	public String toString() {
		return String.format("GearChanged duration:%d", this.getDuration());
	}
}
