package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
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
	// (I.E. JungleNourishment and RageOfTheKeter
	public static int reduceCooldown(Plugin plugin, Player player, int originalCooldown) {
		int level = plugin.mItemStatManager.getInfusionLevel(player, InfusionType.REFRESH);
		double modifiedLevel = DelveInfusionUtils.getModifiedLevel(plugin, player, (int) level);

		// player.sendMessage("Refresh reduced from " + originalCooldown + " ticks to " + (originalCooldown * (1 - 0.02 * modifiedLevel)) + " ticks.");
		return (int) (originalCooldown * (1 - REDUCTION_PER_LEVEL * modifiedLevel));
	}

}
