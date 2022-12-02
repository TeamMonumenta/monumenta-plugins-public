package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.entity.Player;

public class Soothing implements Infusion {

	public static final double HEAL_PER_LEVEL = 0.04;

	@Override
	public String getName() {
		return "Soothing";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.SOOTHING;
	}

	@Override
	public void tick(Plugin plugin, Player player, double value, boolean twoHz, boolean oneHz) {
		if (oneHz) {
			double modifiedLevel = DelveInfusionUtils.getModifiedLevel(plugin, player, (int) value);
			PlayerUtils.healPlayer(plugin, player, modifiedLevel * HEAL_PER_LEVEL);
		}
	}
}
