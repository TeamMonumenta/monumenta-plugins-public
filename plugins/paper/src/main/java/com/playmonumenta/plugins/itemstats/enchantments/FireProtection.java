package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityCombustEvent;

public class FireProtection extends Protection {
	public static final double FIRE_DURATION_REDUCTION = 0.1;

	@Override
	public String getName() {
		return "Fire Protection";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.FIRE_PROTECTION;
	}

	@Override
	public DamageType getType() {
		return DamageType.FIRE;
	}

	@Override
	public int getEPF() {
		return 2;
	}

	@Override
	public void onCombust(Plugin plugin, Player player, double value, EntityCombustEvent event) {
		event.setDuration(getFireDuration(event.getDuration(), value));
	}

	public static int getFireDuration(int ticks, double value) {
		return (int) (ticks * (1 - FIRE_DURATION_REDUCTION * value));
	}

}
