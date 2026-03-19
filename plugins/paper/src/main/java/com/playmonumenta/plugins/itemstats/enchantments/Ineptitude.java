package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
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

	@Override
	public void onAbilityCast(Plugin plugin, Player player, double value, AbilityCastEvent event) {
		event.setCooldown((int) (event.getCooldown() * getCooldownPercentage(value)));
	}

	public static double getCooldownPercentage(double level) {
		return Math.pow(1 + COOLDOWN_INCREASE_PER_LEVEL, level);
	}
}
