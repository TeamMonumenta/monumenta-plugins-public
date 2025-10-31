package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.events.DamageEvent;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class OnDamageTrigger extends Trigger {
	public static final String IDENTIFIER = "ON_DAMAGE";

	private final @Nullable String mCustomAbilityName;
	private final @Nullable DamageEvent.DamageType mDamageType;
	private final long mTotalDamage;
	private double mCurrentDamage = 0;

	public OnDamageTrigger(String name, long damage) {
		mTotalDamage = damage;
		mCustomAbilityName = name;
		mDamageType = null;
	}

	public OnDamageTrigger(DamageEvent.DamageType type, long damage) {
		mTotalDamage = damage;
		mDamageType = type;
		mCustomAbilityName = null;
	}

	@Override
	public boolean onDamage(LivingEntity boss, LivingEntity damagee, DamageEvent event) {
		if (mDamageType == event.getType()) {
			mCurrentDamage += event.getDamage();
		} else if ((event.getBossSpellName() != null && event.getBossSpellName().equals(mCustomAbilityName)) || "ALL".equals(mCustomAbilityName)) {
			mCurrentDamage += event.getDamage();
		}

		return mCurrentDamage >= mTotalDamage;
	}

	@Override
	public boolean test(LivingEntity boss) {
		return mCurrentDamage >= mTotalDamage;
	}

	@Override
	public void reset(LivingEntity boss) {
		mCurrentDamage = 0;
	}

}
