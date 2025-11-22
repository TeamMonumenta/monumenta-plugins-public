package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class OnHurtTrigger extends Trigger {
	public static final String IDENTIFIER = "ON_HURT";

	private final @Nullable ClassAbility mClassAbility;
	private final @Nullable DamageEvent.DamageType mDamageType;
	private final double mTotalDamage;
	private double mCurrentDamage = 0;


	public OnHurtTrigger(double totalDamage) {
		mTotalDamage = totalDamage;
		mDamageType = null;
		mClassAbility = null;
	}

	public OnHurtTrigger(DamageEvent.DamageType type, double totalDamage) {
		mTotalDamage = totalDamage;
		mDamageType = type;
		mClassAbility = null;
	}

	public OnHurtTrigger(ClassAbility ability, double totalDamage) {
		mTotalDamage = totalDamage;
		mClassAbility = ability;
		mDamageType = null;
	}


	@Override
	public boolean test(LivingEntity boss) {
		return mCurrentDamage >= mTotalDamage;
	}

	@Override
	public void reset(LivingEntity boss) {
		mCurrentDamage = 0;
	}

	@Override
	public boolean onHurt(LivingEntity boss, @Nullable LivingEntity damager, DamageEvent event) {
		if (event.getAbility() == mClassAbility && mClassAbility != null) {
			mCurrentDamage += event.getFinalDamage(true);
		} else if (event.getType() == mDamageType) {
			mCurrentDamage += event.getFinalDamage(true);
		} else if (mClassAbility == null && mDamageType == null) {
			mCurrentDamage += event.getFinalDamage(true);
		}

		return mCurrentDamage >= mTotalDamage;
	}


}
