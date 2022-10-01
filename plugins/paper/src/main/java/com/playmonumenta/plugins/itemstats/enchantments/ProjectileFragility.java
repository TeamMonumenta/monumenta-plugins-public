package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;

public class ProjectileFragility extends Protection {

	@Override
	public String getName() {
		return "Projectile Fragility";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.PROJECTILE_FRAGILITY;
	}

	@Override
	public DamageType getType() {
		return DamageType.PROJECTILE;
	}

	@Override
	public int getEPF() {
		return -2;
	}

}
