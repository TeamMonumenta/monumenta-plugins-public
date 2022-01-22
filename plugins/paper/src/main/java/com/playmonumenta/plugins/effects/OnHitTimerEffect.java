package com.playmonumenta.plugins.effects;

public class OnHitTimerEffect extends Effect {

	public OnHitTimerEffect(int duration) {
		super(duration);
	}

	@Override
	public String toString() {
		return String.format("OnHitTimerEffect duration:%d", this.getDuration());
	}
}
