package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.utils.MMLog;
import org.bukkit.entity.LivingEntity;

public class AddAbilityAction implements Action {
	public static final String IDENTIFIER = "ADD_ABILITY";

	private final String mAbility;

	public AddAbilityAction(String ability) {
		mAbility = ability;
	}

	@Override
	public void runAction(LivingEntity boss) {
		try {
			BossManager.createBoss(null, boss, mAbility);
		} catch (Exception e) {
			MMLog.warning("[BossTriggerAction] AddAbilityAction | exception while creating ability for boss: " + boss.getName() + " ability: " + mAbility + " reason: " + e.getMessage());
		}
	}
}
