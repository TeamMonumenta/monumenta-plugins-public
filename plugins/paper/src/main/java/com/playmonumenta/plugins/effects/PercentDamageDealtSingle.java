package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.events.DamageEvent;
import org.bukkit.entity.LivingEntity;

public class PercentDamageDealtSingle extends PercentDamageDealt {

	private boolean mHasDoneDamage;

	public PercentDamageDealtSingle(int duration, double amount) {
		super(duration, amount);
		mHasDoneDamage = false;
	}

	@Override
	public double getMagnitude() {
		return (mHasDoneDamage ? 0 : Math.abs(mAmount));
	}


	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		if (!mHasDoneDamage) {
			mHasDoneDamage = true;
			event.setDamage(event.getDamage() * Math.max(0, 1 + mAmount));
		}
	}

	@Override public String toString() {
		return String.format("PercentDamageDealt duration:%d amount:%f", this.getDuration(), mAmount);
	}
}
