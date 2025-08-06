package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.entity.Player;

public class Poise implements Enchantment {
	public static final double MIN_HEALTH_PERCENT = 0.9;
	public static final double HALF_BONUS_PERCENT = 0.7;

	@Override
	public String getName() {
		return "Poise";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.POISE;
	}

	public static double applyPoise(DamageEvent event, Plugin plugin, Player player) {
		double health = player.getHealth() / EntityUtils.getMaxHealth(player);
		if (health >= MIN_HEALTH_PERCENT) {
			return plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.POISE);
		} else if (health >= HALF_BONUS_PERCENT) {
			return plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.POISE) / 2f;
		} else {
			return 0;
		}
	}

}
