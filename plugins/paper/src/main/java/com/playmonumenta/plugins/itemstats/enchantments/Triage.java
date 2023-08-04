package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;

public class Triage implements Enchantment {

	@Override
	public String getName() {
		return "Triage";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.TRIAGE;
	}

}
