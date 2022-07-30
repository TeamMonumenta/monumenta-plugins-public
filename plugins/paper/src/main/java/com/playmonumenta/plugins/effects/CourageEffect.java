package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.events.DamageEvent;
import java.util.EnumSet;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.Nullable;

// Reduce incoming damage based on charges
public class CourageEffect extends Effect {

	private final @Nullable EnumSet<DamageEvent.DamageType> mAffectedDamageTypes;

	private final double mAmount;
	private int mCharges;
	private int mTickWhenHit = 0;

	public CourageEffect(int duration, double amount, int charges, @Nullable EnumSet<DamageEvent.DamageType> affectedDamageTypes) {
		super(duration);
		mAmount = amount;
		mAffectedDamageTypes = affectedDamageTypes;
		mCharges = charges;
	}

	public CourageEffect(int duration, double amount, int charges) {
		this(duration, amount, charges, null);
	}

	@Override
	public double getMagnitude() {
		return mAmount;
	}

	@Override
	public void onHurt(LivingEntity entity, DamageEvent event) {
		if (mAffectedDamageTypes == null || mAffectedDamageTypes.contains(event.getType())) {
			double amount = mAmount;

			int currentTick = entity.getTicksLived();

			// Check if the last hit was larger than 0.5 seconds - meaning we need to expend a charge for this hit.
			if (currentTick - mTickWhenHit > 10) {
				mCharges -= 1;

				event.setDamage(event.getDamage() * (1 - amount));
				mTickWhenHit = currentTick;

				if (mCharges == 0) {
					mDuration = 10; // We need the effect to last 0.5 seconds for the last charge.
				}
			} else {
				// Therefore this should mean that the last hit is within the 0.5 seconds, so mitigate it
				// without spending charges
				event.setDamage(event.getDamage() * (1 - amount));
			}
		}
	}

	@Override
	public String toString() {
		String types = "any";
		if (mAffectedDamageTypes != null) {
			types = "";
			for (DamageEvent.DamageType type : mAffectedDamageTypes) {
				if (!types.isEmpty()) {
					types += ",";
				}
				types += type.name();
			}
		}
		return String.format("LiquidCourageEffect duration:%d types:%s charges:%d amount:%f", this.getDuration(), types, mCharges, mAmount);
	}
}
