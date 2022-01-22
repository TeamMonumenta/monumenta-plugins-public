package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import org.bukkit.entity.LivingEntity;

import java.util.EnumSet;

public class FlatDamageDealt extends Effect {

	private final double mAmount;
	private final EnumSet<DamageType> mAffectedDamageTypes;

	public FlatDamageDealt(int duration, double amount, EnumSet<DamageType> affectedDamageTypes) {
		super(duration);
		mAmount = amount;
		mAffectedDamageTypes = affectedDamageTypes;
	}

	public FlatDamageDealt(int duration, double amount) {
		this(duration, amount, null);
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		if (mAffectedDamageTypes == null || mAffectedDamageTypes.contains(event.getType())) {
			event.setDamage(event.getDamage() + mAmount);
		}
	}

	@Override
	public String toString() {
		String types = "any";
		if (mAffectedDamageTypes != null) {
			types = "";
			for (DamageType type : mAffectedDamageTypes) {
				if (!types.isEmpty()) {
					types += ",";
				}
				types += type.name();
			}
		}
		return String.format("FlatDamageDealt duration:%d types:%s amount:%f", this.getDuration(), types, mAmount);
	}
}
