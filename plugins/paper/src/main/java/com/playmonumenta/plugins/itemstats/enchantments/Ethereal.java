package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.OnHitTimerEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class Ethereal implements Enchantment {
	public static final int PAST_HIT_DURATION_TIME = (int) (1.5 * 20);
	private static final String ETHEREAL_EFFECT_NAME = "EtherealEffect";

	@Override
	public String getName() {
		return "Ethereal";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.ETHEREAL;
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.isBlocked() || !event.getType().isDefendable()) {
			return;
		}
		plugin.mEffectManager.clearEffects(player, ETHEREAL_EFFECT_NAME);
		// dummy amount (only used for inure)
		plugin.mEffectManager.addEffect(player, ETHEREAL_EFFECT_NAME, new OnHitTimerEffect(PAST_HIT_DURATION_TIME));
	}

	public static double applyEthereal(DamageEvent event, Plugin plugin, Player player) {
		if (plugin.mEffectManager.hasEffect(player, ETHEREAL_EFFECT_NAME)) {
			return plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.ETHEREAL);
		} else {
			return 0;
		}
	}

}
