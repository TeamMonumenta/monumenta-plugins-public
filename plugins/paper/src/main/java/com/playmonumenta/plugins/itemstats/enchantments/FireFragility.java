package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.jetbrains.annotations.NotNull;

public class FireFragility extends Protection {

	@Override
	public @NotNull String getName() {
		return "Fire Fragility";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.FIRE_FRAGILITY;
	}

	@Override
	public DamageType getType() {
		return DamageType.FIRE;
	}

	@Override
	public int getEPF() {
		return -2;
	}

}
