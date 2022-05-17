package com.playmonumenta.plugins.effects;

public class FirstStrikeCooldown extends Effect {

	public FirstStrikeCooldown(int duration) {
		super(duration);
	}

	@Override
	public String toString() {
		return String.format("FirstStrikeCooldown duration:%d", this.getDuration());
	}
}
