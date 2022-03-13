package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.entity.Player;

public class Poise implements Enchantment {

	private static final double ARMOR_BONUS_PER_LEVEL = 0.2;
	private static final double MIN_HEALTH_PERCENT = 0.9;

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
			return plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.POISE) * ARMOR_BONUS_PER_LEVEL;
		} else {
			return 0;
		}
	}

}
