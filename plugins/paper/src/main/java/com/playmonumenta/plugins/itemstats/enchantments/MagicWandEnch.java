package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import java.util.EnumSet;

public class MagicWandEnch implements Enchantment {

	@Override
	public String getName() {
		return "Magic Wand";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.MAGIC_WAND;
	}

	@Override
	public EnumSet<ItemStatUtils.Slot> getSlots() {
		return EnumSet.of(ItemStatUtils.Slot.MAINHAND);
	}

}
