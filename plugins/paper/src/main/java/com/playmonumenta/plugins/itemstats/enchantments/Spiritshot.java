package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;

public class Spiritshot implements Enchantment {
	@Override
	public String getName() {
		return "Spiritshot";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.SPIRITSHOT;
	}
}
