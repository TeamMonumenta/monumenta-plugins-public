package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils;

public class NoGlint implements Enchantment {

	@Override
	public String getName() {
		return "NoGlint";
	}

	@Override
	public ItemStatUtils.EnchantmentType getEnchantmentType() {
		return ItemStatUtils.EnchantmentType.NO_GLINT;
	}
}
