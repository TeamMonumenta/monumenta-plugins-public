package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Aptitude implements Enchantment {
	public static final double COOLDOWN_REDUCTION_PER_LEVEL = 0.05;

	@Override
	public @NotNull String getName() {
		return "Aptitude";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.APTITUDE;
	}

	public static double getCooldownPercentage(Plugin plugin, Player player) {
		return getCooldownPercentage(plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.APTITUDE));
	}

	public static double getCooldownPercentage(double level) {
		return Math.pow(1 - COOLDOWN_REDUCTION_PER_LEVEL, level) - 1;
	}
}
