package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.PhasesManagerBoss;
import org.bukkit.entity.LivingEntity;

public class FlagSetAction implements Action {
	public static final String IDENTIFIER = "FLAG";

	private final String mKeyTrigger;
	private final boolean mState;

	public FlagSetAction(String key, boolean state) {
		mKeyTrigger = key;
		mState = state;
	}

	@Override
	public void runAction(LivingEntity boss) {
		PhasesManagerBoss phaseManagerBoss = BossManager.getInstance().getBoss(boss, PhasesManagerBoss.class);
		if (phaseManagerBoss != null) {
			phaseManagerBoss.onFlagTrigger(mKeyTrigger, mState);
		}
	}

}
