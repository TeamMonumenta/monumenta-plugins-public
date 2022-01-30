package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityCombustEvent;
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

	@Override
	public void onCombust(Plugin plugin, Player player, double value, EntityCombustEvent event) {
		event.setDuration((int) (event.getDuration() - (event.getDuration() * 0.1 * value)));
	}

}
