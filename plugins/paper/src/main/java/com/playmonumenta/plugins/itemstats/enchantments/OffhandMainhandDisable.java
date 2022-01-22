package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;

public class OffhandMainhandDisable implements Enchantment {

	@Override
	public String getName() {
		return "OffhandMainhandDisable";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.OFFHAND_MAINHAND_DISABLE;
	}

	@Override
	public double getPriorityAmount() {
		// Set priority to ABSOLUTE FIRST
		return -10000;
	}
}
