package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;

public class AilmentFragility extends Protection {

	@Override
	public String getName() {
		return "Ailment Fragility";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.AILMENT_FRAGILITY;
	}

	@Override
	public DamageType getType() {
		return DamageType.AILMENT;
	}

	@Override
	public int getEPF() {
		return -2;
	}
}
