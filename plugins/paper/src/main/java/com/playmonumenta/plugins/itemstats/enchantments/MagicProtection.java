package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.jetbrains.annotations.NotNull;

public class MagicProtection extends Protection {

	@Override
	public @NotNull String getName() {
		return "Magic Protection";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.MAGIC_PROTECTION;
	}

	@Override
	public DamageType getType() {
		return DamageType.MAGIC;
	}

	@Override
	public int getEPF() {
		return 2;
	}

}
