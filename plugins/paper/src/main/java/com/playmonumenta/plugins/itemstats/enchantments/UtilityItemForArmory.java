package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;

public class UtilityItemForArmory implements Enchantment {
	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.UTILITY_ITEM_FOR_ARMORY;
	}

	@Override
	public String getName() {
		return "Utility Item For Armory";
	}
}
