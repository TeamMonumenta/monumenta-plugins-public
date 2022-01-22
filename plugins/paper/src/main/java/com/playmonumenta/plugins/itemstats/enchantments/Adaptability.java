package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.jetbrains.annotations.NotNull;

public class Adaptability implements Enchantment {

	@Override
	public @NotNull String getName() {
		return "Adaptability";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.ADAPTABILITY;
	}

}
