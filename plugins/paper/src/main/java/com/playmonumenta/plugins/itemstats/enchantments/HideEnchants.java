package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;

public class HideEnchants implements Enchantment {

	@Override
	public String getName() {
		return "HideEnchants";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.HIDE_ENCHANTS;
	}

}
