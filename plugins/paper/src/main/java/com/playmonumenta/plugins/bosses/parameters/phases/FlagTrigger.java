package com.playmonumenta.plugins.bosses.parameters.phases;

import org.bukkit.entity.LivingEntity;

public class FlagTrigger extends Trigger {
	public static final String IDENTIFIER = "FLAG";

	private final String mKeyString;
	private boolean mState;

	public FlagTrigger(String key, boolean startingState) {
		mKeyString = key;
		mState = startingState;
	}

	@Override
	public boolean test(LivingEntity boss) {
		return mState;
	}

	@Override
	public void reset(LivingEntity boss) {

	}

	@Override
	public boolean tick(LivingEntity boss, int ticks) {
		return mState;
	}

	@Override
	public boolean flag(LivingEntity boss, String key, boolean state) {
		if (key.equals(mKeyString)) {
			mState = state;
			return true;
		}
		return false;
	}

}
