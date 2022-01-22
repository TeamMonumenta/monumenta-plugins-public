package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.jetbrains.annotations.NotNull;

public class FireProtection extends Protection {

	@Override
	public @NotNull String getName() {
		return "Fire Protection";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.FIRE_PROTECTION;
	}

	@Override
	protected DamageType getType() {
		return DamageType.FIRE;
	}

	@Override
	protected int getEPF() {
		return 2;
	}

}
