package com.playmonumenta.plugins.itemstats.infusions;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;

public class Pennate implements Infusion {

	private static final double REDUCT_PCT_PER_LEVEL = 0.05;

	@Override
	public String getName() {
		return "Pennate";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.PENNATE;
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event) {
		double modifiedLevel = DelveInfusionUtils.getModifiedLevel(plugin, player, (int) value);
		if (event.getType() == DamageType.FALL) {
			event.setDamage(event.getDamage() * (1.0 - (REDUCT_PCT_PER_LEVEL * modifiedLevel)));
		}
	}
}
