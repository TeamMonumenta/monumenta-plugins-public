package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import org.bukkit.entity.LivingEntity;

public class ForceCastAction implements Action {
	public static final String IDENTIFIER = "FORCE_CAST";

	private final String mAbility;

	public ForceCastAction(String ability) {
		mAbility = ability;
	}


	@Override
	public void runAction(LivingEntity boss) {
		if (EntityUtils.shouldCancelSpells(boss)) {
			return;
		}

		BossManager manager = BossManager.getInstance();
		if (manager != null) {
			List<BossAbilityGroup> abilityList = manager.getAbilities(boss);
			for (BossAbilityGroup ability : abilityList) {
				if (ability.getIdentityTag().equals(mAbility)) {
					ability.forceCastRandomSpell();
				}
			}
		}
	}

}
