package com.playmonumenta.plugins.effects;

public class ThuribleBonusHealing extends Effect {

	private final double mLevel;

	public ThuribleBonusHealing(int duration, double level) {
		super(duration);
		mLevel = level;
	}

	@Override
	public double getMagnitude() {
		return mLevel;
	}

	@Override
	public String toString() {
		return String.format("ThuribleBonusHealing duration=%d healing=%f", this.getDuration(), mLevel);
	}
}
