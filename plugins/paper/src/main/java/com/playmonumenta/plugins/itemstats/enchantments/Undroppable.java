package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import org.bukkit.inventory.ItemStack;

public class Undroppable implements Enchantment {
	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.UNDROPPABLE;
	}

	@Override
	public String getName() {
		return "Undroppable";
	}

	public static boolean isUndroppable(ItemStack item) {
		return ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.UNDROPPABLE) != 0;
	}
}
