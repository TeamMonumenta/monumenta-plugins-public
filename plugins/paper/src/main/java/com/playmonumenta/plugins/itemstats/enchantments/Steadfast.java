package com.playmonumenta.plugins.itemstats.enchantments;

import java.util.NavigableSet;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;

public class Steadfast implements Enchantment {

	private static final double ARMOR_BONUS_PER_LEVEL = 0.2;
	private static final double BONUS_SCALING_RATE = 0.25;
	private static final double SLOW_SCALING_RATE = -0.125;
	private static final String PERCENT_SPEED_EFFECT_NAME = "SteadfastPercentSpeedEffect";
	private static final int DURATION = 6;

	@Override
	public String getName() {
		return "Steadfast";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.STEADFAST;
	}

	@Override
	public void tick(Plugin plugin, Player player, double level, boolean twoHertz, boolean oneHertz) {
		double maxHealth = EntityUtils.getMaxHealth(player);
		double healthMissing = 1 - (maxHealth - player.getHealth()) / maxHealth;
		NavigableSet<Effect> speedEffects = plugin.mEffectManager.getEffects(player, PERCENT_SPEED_EFFECT_NAME);
		if (speedEffects != null) {
			for (Effect effect : speedEffects) {
				effect.setDuration(DURATION);
			}
		} else {
			plugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(DURATION, Math.max(-0.1, SLOW_SCALING_RATE * healthMissing), PERCENT_SPEED_EFFECT_NAME));
		}
	}

	public static double applySteadfast(DamageEvent event, Plugin plugin, Player player) {
		double healthMissing = 1 - (EntityUtils.getMaxHealth(player) - player.getHealth()) / EntityUtils.getMaxHealth(player);
		double armorBonus = Math.min(ARMOR_BONUS_PER_LEVEL, BONUS_SCALING_RATE * healthMissing);
		return plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.STEADFAST) * armorBonus;
	}

}
