package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.EnumSet;
import java.util.function.BiPredicate;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PercentDamageDealt extends Effect {
	public static final String GENERIC_NAME = "PercentDamageDealt";

	protected final double mAmount;
	protected final @Nullable EnumSet<DamageType> mAffectedDamageTypes;
	protected final int mPriority;
	private @Nullable BiPredicate<LivingEntity, LivingEntity> mPredicate = null;

	public PercentDamageDealt(int duration, double amount, @Nullable EnumSet<DamageType> affectedDamageTypes, int priority, @Nullable BiPredicate<LivingEntity, LivingEntity> predicate) {
		super(duration);
		mAmount = amount;
		mAffectedDamageTypes = affectedDamageTypes;
		mPriority = priority;
		mPredicate = predicate;
	}

	public PercentDamageDealt(int duration, double amount) {
		this(duration, amount, null, 0, null);
	}

	public PercentDamageDealt(int duration, double amount, @Nullable EnumSet<DamageType> affectedDamageTypes) {
		this(duration, amount, affectedDamageTypes, 0, null);
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
	public boolean isDebuff() {
		return mAmount < 0;
	}

	public EnumSet<DamageType> getAffectedDamageTypes() {
		return mAffectedDamageTypes;
	}

	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		if (mPredicate != null && !mPredicate.test(entity, enemy)) {
			return;
		}
		if (mAffectedDamageTypes == null || mAffectedDamageTypes.contains(event.getType())) {
			event.setDamage(event.getDamage() * Math.max(0, 1 + mAmount));
		}
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(mAmount) + " Damage Dealt";
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
