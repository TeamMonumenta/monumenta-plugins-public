package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.EnumSet;
import java.util.stream.Collectors;
import org.bukkit.entity.LivingEntity;

public class PercentAbilityDamageReceived extends Effect {
	public static final String effectID = "PercentAbilityDamageReceived";
	private final double mAmount;
	private final EnumSet<ClassAbility> mAffectedAbilities;

	public PercentAbilityDamageReceived(int duration, double amount, EnumSet<ClassAbility> affectedAbilities) {
		super(duration, effectID);
		mAmount = amount;
		mAffectedAbilities = affectedAbilities;
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	@Override
	public boolean isDebuff() {
		return mAmount > 0;
	}

	@Override
	public boolean isBuff() {
		return mAmount < 0;
	}

	@Override
	public void onHurt(LivingEntity entity, DamageEvent event) {
		if (event.getAbility() != null && mAffectedAbilities.contains(event.getAbility())) {
			double amount = mAmount;
			if (EntityUtils.isBoss(entity) && amount > 0) {
				amount = amount / 2;
			}
			event.setDamage(event.getDamage() * (1 + amount));
		}
	}

	@Override
	public String toString() {
		String types = mAffectedAbilities.stream().map(Enum::name).collect(Collectors.joining(","));
		return String.format("PercentAbilityDamageReceived duration:%d types:%s amount:%f", this.getDuration(), types, mAmount);
	}
}
