package com.playmonumenta.plugins.effects;

import org.bukkit.entity.Entity;

public class RecoilDisable extends Effect {

	private final double mAmount;

	public RecoilDisable(int duration, int amount) {
		super(duration);
		mAmount = amount;
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	@Override
	public String toString() {
		return String.format("RecoilDisable duration:%d amount:%f", this.getDuration(), mAmount);
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (fourHertz) {
			if (entity.isOnGround()) {
				setDuration(0);
			}
		}
	}
}
