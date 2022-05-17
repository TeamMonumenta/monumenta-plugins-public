package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;

public class MeleeFragility extends Protection {

	@Override
	public String getName() {
		return "Melee Fragility";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.MELEE_FRAGILITY;
	}

	@Override
	public DamageType getType() {
		return DamageType.MELEE;
	}

	@Override
	public int getEPF() {
		return -2;
	}

}
