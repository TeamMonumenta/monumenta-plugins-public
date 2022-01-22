package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerExpChangeEvent;

public class Intuition implements Enchantment {
	private static final double INTUITION_MULTIPLIER = 1.5;

	@Override
	public String getName() {
		return "Intuition";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.INTUITION;
	}

	@Override
	public void onExpChange(Plugin plugin, Player player, double level, PlayerExpChangeEvent event) {
		event.setAmount((int)(event.getAmount() * INTUITION_MULTIPLIER));
	}
}
