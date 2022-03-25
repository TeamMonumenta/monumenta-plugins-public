package com.playmonumenta.plugins.effects;

public abstract class SingleArgumentEffect extends Effect {
	protected final double mAmount;

	public SingleArgumentEffect(int duration, double amount) {
		super(duration);
		mAmount = amount;
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}
}
