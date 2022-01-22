package com.playmonumenta.plugins.itemstats.enchantments;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;

public class Ineptitude implements Enchantment {
	public static final double COOLDOWN_REDUCTION_PER_LEVEL = 0.05;

	@Override
	public String getName() {
		return "Ineptitude";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.INEPTITUDE;
	}

	public static double getCooldownPercentage(Plugin plugin, Player player) {
		return COOLDOWN_REDUCTION_PER_LEVEL * plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.INEPTITUDE);
	}
}
