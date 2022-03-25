package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;

public class FeatherFalling extends Protection {

	@Override
	public String getName() {
		return "Feather Falling";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.FEATHER_FALLING;
	}

	@Override
	public DamageType getType() {
		return DamageType.FALL;
	}

	@Override
	public int getEPF() {
		return 3;
	}

}
