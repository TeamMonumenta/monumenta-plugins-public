package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;

public class Weightless implements Enchantment {

	@Override
	public String getName() {
		return "Weightless";
	}

	@Override
	public EnchantmentType getEnchantmentType() {

		return EnchantmentType.WEIGHTLESS;
	}
}
