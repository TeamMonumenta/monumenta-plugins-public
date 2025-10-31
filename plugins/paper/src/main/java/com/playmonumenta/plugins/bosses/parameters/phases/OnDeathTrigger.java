package com.playmonumenta.plugins.bosses.parameters.phases;

import org.bukkit.entity.LivingEntity;

public class OnDeathTrigger extends Trigger {
	public static final String IDENTIFIER = "ON_DEATH";

	@Override
	public boolean onDeath(LivingEntity boss) {
		return true;
	}

	@Override
	public boolean test(LivingEntity boss) {
		return false;
	}

	@Override
	public void reset(LivingEntity boss) {

	}

}
