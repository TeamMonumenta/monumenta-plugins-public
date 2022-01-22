package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;

public class CurseOfIrreparability implements Enchantment {

	@Override
	public String getName() {
		return "Curse of Irreparability";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.CURSE_OF_IRREPARIBILITY;
	}

}
