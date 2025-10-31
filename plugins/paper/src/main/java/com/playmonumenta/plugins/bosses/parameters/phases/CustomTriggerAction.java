package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.PhasesManagerBoss;
import org.bukkit.entity.LivingEntity;

public class CustomTriggerAction implements Action {
	public static final String IDENTIFIER = "CUSTOM";

	private final String mKeyTrigger;

	public CustomTriggerAction(String key) {
		mKeyTrigger = key;
	}

	@Override
	public void runAction(LivingEntity boss) {
		PhasesManagerBoss phaseManagerBoss = BossManager.getInstance().getBoss(boss, PhasesManagerBoss.class);
		if (phaseManagerBoss != null) {
			phaseManagerBoss.onCustomTrigger(mKeyTrigger);
		}
	}

}
