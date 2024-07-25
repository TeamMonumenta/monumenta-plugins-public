package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;

public class Relic implements Enchantment {
	@Override
	public String getName() {
		return "Relic";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.RELIC;
	}
}
