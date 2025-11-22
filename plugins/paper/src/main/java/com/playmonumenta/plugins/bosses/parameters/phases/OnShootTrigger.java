package com.playmonumenta.plugins.bosses.parameters.phases;

import org.bukkit.entity.LivingEntity;

public class OnShootTrigger extends Trigger {
	public static final String IDENTIFIER = "ON_SHOOT";

	public OnShootTrigger() {
	}

	@Override
	public boolean onShoot(LivingEntity boss) {
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
