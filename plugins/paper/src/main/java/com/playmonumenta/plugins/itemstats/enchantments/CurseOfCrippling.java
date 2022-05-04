package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.entity.Player;

public class CurseOfCrippling implements Enchantment {
	private static final int EFFECT_DURATION = 10;
	private static final double PERCENT_SPEED_PER_LEVEL = -0.1;
	private static final double PERCENT_DAMAGE_PER_LEVEL = -0.1;
	private static final String PERCENT_SPEED_EFFECT_NAME = "CripplingPercentSpeedEffect";
	private static final String PERCENT_DAMAGE_EFFECT_NAME = "CripplingPercentDamageEffect";
	private static final double MIN_HEALTH_PERCENT = 0.5;

	@Override
	public String getName() {
		return "Curse of Crippling";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.CURSE_OF_CRIPPLING;
	}

	@Override
	public void tick(Plugin plugin, Player player, double value, boolean twoHz, boolean oneHz) {
		double health = player.getHealth() / EntityUtils.getMaxHealth(player);
		if (health <= MIN_HEALTH_PERCENT) {
			plugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(EFFECT_DURATION, value * PERCENT_SPEED_PER_LEVEL, PERCENT_SPEED_EFFECT_NAME));
			plugin.mEffectManager.addEffect(player, PERCENT_DAMAGE_EFFECT_NAME, new PercentDamageDealt(EFFECT_DURATION, value * PERCENT_DAMAGE_PER_LEVEL));
		} else {
			plugin.mEffectManager.clearEffects(player, PERCENT_SPEED_EFFECT_NAME);
			plugin.mEffectManager.clearEffects(player, PERCENT_DAMAGE_EFFECT_NAME);
		}
	}
}
