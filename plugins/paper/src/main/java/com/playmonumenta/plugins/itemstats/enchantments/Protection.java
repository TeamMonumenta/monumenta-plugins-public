package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import javax.annotation.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public abstract class Protection implements Enchantment {

	private static final double REDUCTION_PER_EPF = 0.96;

	protected abstract DamageType getType();

	protected abstract int getEPF();

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.getType() == getType()) {
			event.setDamage(event.getDamage() * Math.pow(REDUCTION_PER_EPF, value * getEPF()));
		}
	}

}
