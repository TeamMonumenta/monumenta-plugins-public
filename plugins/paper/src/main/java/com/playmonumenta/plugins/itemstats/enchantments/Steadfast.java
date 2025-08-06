package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.entity.Player;

public class Steadfast implements Enchantment {
	public static final double MISSING_HEALTH_MAXIMUM = 0.6;

	@Override
	public String getName() {
		return "Steadfast";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.STEADFAST;
	}

	public static double applySteadfast(DamageEvent event, Plugin plugin, Player player) {
		double maxHealth = EntityUtils.getMaxHealth(player);
		double healthMissing = 1.0 - player.getHealth() / maxHealth;
		// Can give at most 1 level of steadfast at 60% health missing or greater, until then the ratio of missing health to 0.6 gets closer to 1
		double scaledSteadfastLevel = Math.min(1.0, healthMissing / MISSING_HEALTH_MAXIMUM);
		return plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.STEADFAST) * scaledSteadfastLevel;
	}

}
