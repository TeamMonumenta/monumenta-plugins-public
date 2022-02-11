package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.entity.Player;

public class Ineptitude implements Enchantment {
	public static final double COOLDOWN_INCREASE_PER_LEVEL = 0.05;

	@Override
	public String getName() {
		return "Ineptitude";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.INEPTITUDE;
	}

	public static double getCooldownPercentage(Plugin plugin, Player player) {
		return getCooldownPercentage(plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.INEPTITUDE));
	}

	public static double getCooldownPercentage(double level) {
		return Math.pow(1 + COOLDOWN_INCREASE_PER_LEVEL, level) - 1;
	}
}
