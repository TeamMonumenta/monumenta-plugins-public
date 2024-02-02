package com.playmonumenta.plugins.depths.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.effects.OnHitTimerEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public abstract class DepthsCombosAbility extends DepthsAbility {
	private static final String COOLDOWN_EFFECT = "CombosCooldownEffect";
	public static final int COOLDOWN_DURATION = 2 * 20;

	protected final int mHitRequirement;

	private int mComboCount = 0;

	public DepthsCombosAbility(Plugin plugin, Player player, DepthsAbilityInfo<?> info, int baseHitRequirement, String hitRequirementCharm) {
		super(plugin, player, info);
		mHitRequirement = baseHitRequirement + (int) CharmManager.getLevel(player, hitRequirementCharm);
	}

	public boolean triggersCombos(DamageEvent event) {
		return DepthsUtils.isValidComboAttack(event, mPlayer);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (triggersCombos(event) && !(mComboCount == mHitRequirement - 1 && mPlugin.mEffectManager.hasEffect(mPlayer, COOLDOWN_EFFECT))) {
			mComboCount++;
			if (mComboCount >= mHitRequirement) {
				mComboCount = 0;
				mPlugin.mEffectManager.addEffect(mPlayer, COOLDOWN_EFFECT, new OnHitTimerEffect(COOLDOWN_DURATION, 1));
				activate(event, enemy);
			}
		}
		return false;
	}

	public abstract void activate(DamageEvent event, LivingEntity enemy);
}
