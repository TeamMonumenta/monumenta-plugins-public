package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.warlock.reaper.JudgementChain;
import com.playmonumenta.plugins.events.DamageEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class JudgementChainPlayerEffect extends Effect {
	public static final String effectID = "JudgementChainPlayerEffect";

	private final Player mSourcePlayer;

	public JudgementChainPlayerEffect(int duration, Player sourcePlayer) {
		super(duration, effectID);
		mSourcePlayer = sourcePlayer;
	}

	@Override
	public EffectPriority getPriority() {
		return EffectPriority.NORMAL;
	}

	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		JudgementChain chainInstance = Plugin.getInstance().mAbilityManager.getPlayerAbility(mSourcePlayer, JudgementChain.class);
		if (chainInstance != null) {
			chainInstance.passDamage(event);
		}
	}

	@Override
	public String toString() {
		return String.format("JudgementChainPlayerEffect duration:%d", this.getDuration());
	}
}
