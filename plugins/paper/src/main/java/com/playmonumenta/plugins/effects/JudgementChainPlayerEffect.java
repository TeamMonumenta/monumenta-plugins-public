package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.abilities.warlock.reaper.JudgementChain;
import com.playmonumenta.plugins.events.DamageEvent;
import org.bukkit.entity.LivingEntity;

public class JudgementChainPlayerEffect extends Effect {
	public static final String effectID = "JudgementChainPlayerEffect";

	private final JudgementChain mJudgementChain;

	public JudgementChainPlayerEffect(int duration, JudgementChain judgementChain) {
		super(duration, effectID);
		mJudgementChain = judgementChain;
	}

	@Override
	public EffectPriority getPriority() {
		return EffectPriority.NORMAL;
	}

	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		mJudgementChain.passDamage(event);
	}

	@Override
	public String toString() {
		return String.format("JudgementChainPlayerEffect duration:%d", this.getDuration());
	}
}
