package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import java.util.EnumSet;

public class Drilling implements Enchantment {

	@Override
	public String getName() {
		return "Drilling";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.DRILLING;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}
}
