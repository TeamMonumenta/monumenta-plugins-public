package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.OnHitTimerEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import java.util.NavigableSet;
import javax.annotation.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Tempo implements Enchantment {

	private static final double AGIL_BONUS_PER_LEVEL_1 = 0.1;
	private static final double AGIL_BONUS_PER_LEVEL_2 = 0.2;
	private static final int PAST_HIT_DURATION_TIME = 20 * 4;
	private static final int PAST_HIT_DURATION_TIME_HALF = 20 * 2;
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
		if (event.isBlocked()) {
			return;
		}
		if (event.getType() == DamageEvent.DamageType.FIRE || event.getType() == DamageEvent.DamageType.FALL || event.getType() == DamageEvent.DamageType.AILMENT) {
			return;
		}
		plugin.mEffectManager.clearEffects(player, TEMPO_EFFECT_NAME);
		plugin.mEffectManager.addEffect(player, TEMPO_EFFECT_NAME, new OnHitTimerEffect(PAST_HIT_DURATION_TIME));
	}

	public static double applyTempo(DamageEvent event, Plugin plugin, Player player) {
		NavigableSet<Effect> tempo = plugin.mEffectManager.getEffects(player, TEMPO_EFFECT_NAME);
		if (tempo == null) {
			return plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.TEMPO) * AGIL_BONUS_PER_LEVEL_2;
		} else {
			Effect temp = tempo.last();
			if (temp.getDuration() <= PAST_HIT_DURATION_TIME_HALF) {
				return plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.TEMPO) * AGIL_BONUS_PER_LEVEL_1;
			} else {
				return 0;
			}
		}
	}

}
