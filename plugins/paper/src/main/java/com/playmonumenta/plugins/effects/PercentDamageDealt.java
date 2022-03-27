package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import java.util.EnumSet;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PercentDamageDealt extends Effect {
	public static final String GENERIC_NAME = "PercentDamageDealt";

	private final double mAmount;
	private final @Nullable EnumSet<DamageType> mAffectedDamageTypes;
	private final int mPriority;

	public PercentDamageDealt(int duration, double amount, @Nullable EnumSet<DamageType> affectedDamageTypes, int priority) {
		super(duration);
		mAmount = amount;
		mAffectedDamageTypes = affectedDamageTypes;
		mPriority = priority;
	}

	public PercentDamageDealt(int duration, double amount) {
		this(duration, amount, null, 0);
	}

	public PercentDamageDealt(int duration, double amount, @Nullable EnumSet<DamageType> affectedDamageTypes) {
		this(duration, amount, affectedDamageTypes, 0);
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

	public EnumSet<DamageType> getAffectedDamageTypes() {
		return mAffectedDamageTypes;
	}

	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		if (mAffectedDamageTypes == null || mAffectedDamageTypes.contains(event.getType())) {
			event.setDamage(event.getDamage() * Math.max(0, 1 + mAmount));
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
		return String.format("PercentDamageDealt duration:%d types:%s amount:%f", this.getDuration(), types, mAmount);
	}
}
