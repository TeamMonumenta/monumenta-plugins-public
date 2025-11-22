package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.bosses.BossManager;
import org.bukkit.entity.LivingEntity;

public class RemoveAbilityAction implements Action {
	public static final String IDENTIFIER = "REMOVE_ABILITY";
	private final String mAbility;

	public RemoveAbilityAction(String ability) {
		mAbility = ability;
	}

	@Override
	public void runAction(LivingEntity boss) {
		BossManager.getInstance().removeAbility(boss, mAbility);
		boss.removeScoreboardTag(mAbility);
	}

}
