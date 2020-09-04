package com.playmonumenta.plugins.effects;

import java.util.EnumSet;

import org.bukkit.event.entity.EntityDamageEvent;

public class PercentDamageReceived extends Effect {

	private final double mAmount;
	private final EnumSet<EntityDamageEvent.DamageCause> mAffectedDamageCauses;

	public PercentDamageReceived(int duration, double amount, EnumSet<EntityDamageEvent.DamageCause> affectedDamageCauses) {
		super(duration);
		mAmount = amount;
		mAffectedDamageCauses = affectedDamageCauses;
	}

	public PercentDamageReceived(int duration, double amount) {
		this(duration, amount, null);
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	@Override
	public boolean entityReceiveDamageEvent(EntityDamageEvent event) {
		if (mAffectedDamageCauses == null || mAffectedDamageCauses.contains(event.getCause())) {
			event.setDamage(event.getDamage() * (1 + mAmount));
		}

		return true;
	}

}
