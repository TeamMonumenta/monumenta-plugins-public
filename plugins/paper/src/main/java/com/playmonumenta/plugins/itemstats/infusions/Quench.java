package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import org.bukkit.entity.Player;

public class Quench implements Infusion {

	public static final double DURATION_BONUS_PER_LVL = 0.025;

	@Override
	public String getName() {
		return "Quench";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.QUENCH;
	}

	public static double getDurationScaling(Plugin plugin, Player player) {
		double level = plugin.mItemStatManager.getInfusionLevel(player, InfusionType.QUENCH);
		return 1 + DURATION_BONUS_PER_LVL * level;
	}
}
