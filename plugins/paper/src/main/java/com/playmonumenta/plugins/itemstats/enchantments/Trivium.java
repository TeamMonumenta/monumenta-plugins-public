package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;

public class Trivium implements Enchantment {

	@Override
	public String getName() {
		return "Trivium";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.TRIVIUM;
	}

}
