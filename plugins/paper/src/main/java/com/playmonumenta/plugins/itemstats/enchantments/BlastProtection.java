package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.jetbrains.annotations.NotNull;

public class BlastProtection extends Protection {

	@Override
	public @NotNull String getName() {
		return "Blast Protection";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.BLAST_PROTECTION;
	}

	@Override
	public DamageType getType() {
		return DamageType.BLAST;
	}

	@Override
	public int getEPF() {
		return 2;
	}

}
