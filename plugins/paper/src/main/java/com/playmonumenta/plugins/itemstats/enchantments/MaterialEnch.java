package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;

public class MaterialEnch implements Enchantment {

	@Override
	public String getName() {
		return "Material";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.MATERIAL;
	}

}
