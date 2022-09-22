package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
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

	public static double getCooldownPercentage(Plugin plugin, Player player) {
		int level = plugin.mItemStatManager.getInfusionLevel(player, InfusionType.EPOCH);
		return getCooldownPercentage(DelveInfusionUtils.getModifiedLevel(plugin, player, level));
	}

	public static double getCooldownPercentage(double level) {
		return -COOLDOWN_REDUCTION_PER_LEVEL * level;
	}

}
