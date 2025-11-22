package com.playmonumenta.plugins.bosses.parameters.phases;

import org.bukkit.entity.LivingEntity;

public class RandomAction implements Action {
	public static final String IDENTIFIER = "RANDOM";

	private final Action mAction1;
	private final Action mAction2;
	private final double mChance;

	public RandomAction(double chance, Action action1, Action action2) {
		mChance = chance;
		mAction1 = action1;
		mAction2 = action2;
	}


	@Override
	public void runAction(LivingEntity boss) {
		if (Math.random() < mChance) {
			mAction1.runAction(boss);
		} else {
			mAction2.runAction(boss);
		}
	}

}
