package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.EnumSet;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PercentDamageReceived extends Effect {
	public static final String GENERIC_NAME = "PercentDamageReceived";

	private final double mAmount;
	private final @Nullable EnumSet<DamageType> mAffectedDamageTypes;

	public PercentDamageReceived(int duration, double amount, @Nullable EnumSet<DamageType> affectedDamageTypes) {
		super(duration);
		mAmount = amount;
		mAffectedDamageTypes = affectedDamageTypes;
	}

	public PercentDamageReceived(int duration, double amount) {
		this(duration, amount, null);
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	public EnumSet<DamageType> getAffectedDamageTypes() {
		return mAffectedDamageTypes;
	}

	@Override
	public void onHurt(LivingEntity entity, DamageEvent event) {
		if (event.getType() != DamageType.TRUE && (mAffectedDamageTypes == null || mAffectedDamageTypes.contains(event.getType()))) {
			double amount = mAmount;
			if (EntityUtils.isBoss(entity) && amount > 0) {
				amount = amount / 2;
			}
			event.setDamage(event.getDamage() * (1 + amount));
		}
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(-mAmount) + " Resistance";
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
		return String.format("PercentDamageReceived duration:%d types:%s amount:%f", this.getDuration(), types, mAmount);
	}
}
