package com.playmonumenta.plugins.effects;

public class ThuribleBonusHealing extends SingleArgumentEffect {

	public ThuribleBonusHealing(int duration, double amount) {
		super(duration, amount);
	}

	@Override
	public String toString() {
		return String.format("ThuribleBonusHealing duration=%d healing=%f", this.getDuration(), mAmount);
	}
}
