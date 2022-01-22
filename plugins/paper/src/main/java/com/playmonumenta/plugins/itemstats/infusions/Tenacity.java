package com.playmonumenta.plugins.itemstats.infusions;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;

public class Tenacity implements Infusion {

	private static final double REDUCT_PCT_PER_LEVEL = 0.005;

	@Override
	public String getName() {
		return "Tenacity";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.TENACITY;
	}

	@Override
	public void onHurtByEntity(Plugin plugin, Player player, double value, DamageEvent event, Entity damager) {
		double reductionPct = value * REDUCT_PCT_PER_LEVEL;
		event.setDamage(event.getDamage() * (1.0 - reductionPct));
	}
}
