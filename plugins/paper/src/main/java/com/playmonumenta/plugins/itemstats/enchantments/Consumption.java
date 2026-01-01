package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import org.jetbrains.annotations.NotNull;

public class Consumption implements Enchantment {

	@Override
	public @NotNull String getName() {
		return "Consumption";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.CONSUMPTION;
	}
}
