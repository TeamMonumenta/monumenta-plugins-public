package com.playmonumenta.plugins.effects;

import org.bukkit.event.entity.EntityRegainHealthEvent;

public class PercentHeal extends Effect {

	private final double mAmount;

	public PercentHeal(int duration, double amount) {
		super(duration);
		mAmount = amount;
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	@Override
	public boolean entityRegainHealthEvent(EntityRegainHealthEvent event) {
		event.setAmount(event.getAmount() * (1 + mAmount));
		return mAmount > -1;
	}

}
