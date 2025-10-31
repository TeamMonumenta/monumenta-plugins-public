package com.playmonumenta.plugins.bosses.parameters.phases;

import org.bukkit.entity.LivingEntity;

public class CustomTrigger extends Trigger {
	public static final String IDENTIFIER = "CUSTOM";

	private final String mKeyString;
	private final boolean mOneTime;
	private boolean mHasTriggedOnce = false;

	public CustomTrigger(String key, boolean oneTime) {
		mKeyString = key;
		mOneTime = oneTime;
	}

	@Override
	public boolean test(LivingEntity boss) {
		return mHasTriggedOnce;
	}

	@Override
	public void reset(LivingEntity boss) {
		mHasTriggedOnce = false;
	}

	@Override
	public boolean custom(LivingEntity boss, String key) {
		if (key.equals(mKeyString)) {
			if (!mOneTime) {
				mHasTriggedOnce = true;
			}
			return true;
		}
		return false;
	}

}
