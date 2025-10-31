package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.OnHitTimerEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class Tempo implements Enchantment {
	private static final double TEMPO_HALVED_MULTIPLIER = 0.5;
	public static final int PAST_HIT_DURATION_TIME = 20 * 4;
	public static final int PAST_HIT_DURATION_TIME_HALF = 50; // 50 ticks = 2.5 seconds
	private static final String TEMPO_EFFECT_NAME = "TempoEffect";

	@Override
	public String getName() {
		return "Tempo";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.TEMPO;
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.isBlocked()
			|| !event.getType().isDefendable()
			|| event.getType() == DamageEvent.DamageType.FIRE
			|| event.getType() == DamageEvent.DamageType.FALL) {
			return;
		}
		plugin.mEffectManager.clearEffects(player, TEMPO_EFFECT_NAME);
		plugin.mEffectManager.addEffect(player, TEMPO_EFFECT_NAME, new OnHitTimerEffect(PAST_HIT_DURATION_TIME));
	}

	public static double applyTempo(DamageEvent event, Plugin plugin, Player player) {
		Effect tempo = plugin.mEffectManager.getActiveEffect(player, TEMPO_EFFECT_NAME);
		if (tempo == null) {
			return plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.TEMPO);
		} else if (tempo.getDuration() <= PAST_HIT_DURATION_TIME - PAST_HIT_DURATION_TIME_HALF) {
			// Only if tempo has 0-20 ticks remaining (80 - 60)
			return plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.TEMPO) * TEMPO_HALVED_MULTIPLIER;
		} else {
			return 0;
		}
	}

}
