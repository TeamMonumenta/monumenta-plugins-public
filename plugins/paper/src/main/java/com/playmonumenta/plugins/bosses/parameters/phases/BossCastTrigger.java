package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import org.bukkit.entity.LivingEntity;

public class BossCastTrigger extends Trigger {
	//TODO - when we are going to refactor the old boss ability code this trigger need to handle custom names for abilities instead of tags
	public static final String IDENTIFIER = "ON_CAST";

	private final String mBossAbilityTag;

	public BossCastTrigger(String tag) {
		mBossAbilityTag = tag;
	}

	@Override
	public boolean onBossCastAbility(LivingEntity boss, SpellCastEvent event) {
		return event.getBossAbilityGroup().getIdentityTag().equals(mBossAbilityTag);
	}

	@Override
	public boolean test(LivingEntity boss) {
		return false;
	}

	@Override
	public void reset(LivingEntity boss) {
		//no reset needed
	}


}
