package com.playmonumenta.plugins.effects;

import java.util.EnumSet;

import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class PercentDamageDealt extends Effect {

	private final double mAmount;
	private final EnumSet<EntityDamageEvent.DamageCause> mAffectedDamageCauses;
	private final int mPriority;

	public PercentDamageDealt(int duration, double amount, EnumSet<EntityDamageEvent.DamageCause> affectedDamageCauses, int priority) {
		super(duration);
		mAmount = amount;
		mAffectedDamageCauses = affectedDamageCauses;
		mPriority = priority;
	}

	public PercentDamageDealt(int duration, double amount) {
		this(duration, amount, null, 0);
	}

	public PercentDamageDealt(int duration, double amount, EnumSet<EntityDamageEvent.DamageCause> affectedDamageCauses) {
		this(duration, amount, affectedDamageCauses, 0);
	}

	// This needs to trigger before any flat damage
	@Override
	public EffectPriority getPriority() {
		if (mPriority == 1) {
			return EffectPriority.NORMAL;
		} else if (mPriority == 2) {
			return EffectPriority.LATE;
		} else {
			return EffectPriority.EARLY;
		}
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	@Override
	public boolean entityDealDamageEvent(EntityDamageByEntityEvent event) {
		if (mAffectedDamageCauses == null || mAffectedDamageCauses.contains(event.getCause())) {
			event.setDamage(event.getDamage() * (1 + mAmount));
		}

		return true;
	}

	@Override
	public String toString() {
		String causes = "any";
		if (mAffectedDamageCauses != null) {
			causes = "";
			for (EntityDamageEvent.DamageCause cause : mAffectedDamageCauses) {
				if (!causes.isEmpty()) {
					causes += ",";
				}
				causes += cause.name();
			}
		}
		return String.format("PercentDamageDealt duration:%d causes:%s amount:%f", this.getDuration(), causes, mAmount);
	}
}
