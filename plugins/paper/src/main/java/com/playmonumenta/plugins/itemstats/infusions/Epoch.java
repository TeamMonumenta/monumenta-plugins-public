package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import org.bukkit.entity.Player;

public class Epoch implements Infusion {
	public static final double COOLDOWN_REDUCTION_PER_LEVEL = 0.01;

	@Override
	public String getName() {
		return "Epoch";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.EPOCH;
	}

	@Override
	public void onAbilityCast(Plugin plugin, Player player, double value, AbilityCastEvent event) {
		event.setCooldown((int) (event.getCooldown() * getCooldownPercentage(value)));
	}

	public static double getCooldownPercentage(double level) {
		return 1 - COOLDOWN_REDUCTION_PER_LEVEL * level;
	}
}
