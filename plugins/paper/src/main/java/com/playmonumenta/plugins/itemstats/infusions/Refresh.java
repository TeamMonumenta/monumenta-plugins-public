package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import org.bukkit.entity.Player;

public class Refresh implements Infusion {

	public static final double REDUCTION_PER_LEVEL = 0.02;

	@Override
	public String getName() {
		return "Refresh";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.REFRESH;
	}

	// This function needs to be called by infinite food items that utilizes cooldowns.
	// (I.E. JungleNourishment and RageOfTheKeter)
	public static int reduceCooldown(Plugin plugin, Player player, int originalCooldown) {
		double level = plugin.mItemStatManager.getInfusionLevel(player, InfusionType.REFRESH);
		return (int) (originalCooldown * (1 - REDUCTION_PER_LEVEL * level));
	}

}
