package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.jetbrains.annotations.NotNull;

public class EarthAspect implements Enchantment {

	@Override
	public @NotNull String getName() {
		return "Earth Aspect";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.EARTH_ASPECT;
	}

}
