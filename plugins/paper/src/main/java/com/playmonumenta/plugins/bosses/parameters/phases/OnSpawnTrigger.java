package com.playmonumenta.plugins.bosses.parameters.phases;

import org.bukkit.entity.LivingEntity;

public class OnSpawnTrigger extends Trigger {
	public static final String IDENTIFIER = "ON_SPAWN";

	@Override
	public boolean onSpawn(LivingEntity boss) {
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
