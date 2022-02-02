package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;

public class MainhandOffhandDisable implements Enchantment {

	@Override
	public String getName() {
		return "MainhandOffhandDisable";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.MAINHAND_OFFHAND_DISABLE;
	}

}
