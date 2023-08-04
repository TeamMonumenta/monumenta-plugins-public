package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;

public class HideAttributes implements Enchantment {

	@Override
	public String getName() {
		return "HideAttributes";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.HIDE_ATTRIBUTES;
	}

}
