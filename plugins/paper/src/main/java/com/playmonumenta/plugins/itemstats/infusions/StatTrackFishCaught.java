package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class StatTrackFishCaught implements Infusion {
	@Override
	public InfusionType getInfusionType() {
		return InfusionType.STAT_TRACK_FISH_CAUGHT;
	}

	@Override
	public String getName() {
		return "Fish Caught";
	}


	public static void fishCaught(Player player) {
		ItemStack is = player.getInventory().getItemInMainHand();
		StatTrackManager.incrementStat(is, player, InfusionType.STAT_TRACK_FISH_CAUGHT, 1);
	}
}
