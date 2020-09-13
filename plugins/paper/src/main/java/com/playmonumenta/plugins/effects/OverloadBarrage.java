package com.playmonumenta.plugins.effects;

import java.util.EnumSet;

import org.bukkit.event.entity.EntityDamageEvent;

//Used for flat damage additions to all spells when hit by a Overloaded Arcane Barrage.
//Cannot use the generic flat damage effect due to custom effect damage (spells) functioning differently.
public class OverloadBarrage extends Effect {

	private final double mAmount;
	private final EnumSet<EntityDamageEvent.DamageCause> mAffectedDamageCauses;

	public OverloadBarrage(int duration, double amount, EnumSet<EntityDamageEvent.DamageCause> affectedDamageCauses) {
		super(duration);
		mAmount = amount;
		mAffectedDamageCauses = affectedDamageCauses;
	}

	public OverloadBarrage(int duration, double amount) {
		this(duration, amount, null);
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	@Override
	public boolean entityReceiveDamageEvent(EntityDamageEvent event) {
		if (mAffectedDamageCauses == null || mAffectedDamageCauses.contains(event.getCause())) {
			event.setDamage(event.getDamage() + mAmount);
		}

		return true;
	}

}
