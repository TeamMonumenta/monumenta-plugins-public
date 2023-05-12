package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public class Nutriment implements Infusion {

	public static final double HEALING_PERCENT_PER_LEVEL = 0.015;

	@Override
	public String getName() {
		return "Nutriment";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.NUTRIMENT;
	}

	@Override
	public void onRegain(Plugin plugin, Player player, double value, EntityRegainHealthEvent event) {
		event.setAmount(event.getAmount() * getHealingMultiplier(value));
	}

	public static double getHealingMultiplier(double level) {
		return 1 + HEALING_PERCENT_PER_LEVEL * level;
	}

}
