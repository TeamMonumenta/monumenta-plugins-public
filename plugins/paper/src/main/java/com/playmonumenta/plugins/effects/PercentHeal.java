package com.playmonumenta.plugins.effects;

import org.bukkit.event.entity.EntityRegainHealthEvent;

public class PercentHeal extends SingleArgumentEffect {

	public PercentHeal(int duration, double amount) {
		super(duration, amount);
	}

	@Override
	public boolean entityRegainHealthEvent(EntityRegainHealthEvent event) {
		event.setAmount(event.getAmount() * (1 + mAmount));
		return mAmount > -1;
	}

	@Override
	public String toString() {
		return String.format("PercentHeal duration:%d amount:%f", this.getDuration(), mAmount);
	}
}
