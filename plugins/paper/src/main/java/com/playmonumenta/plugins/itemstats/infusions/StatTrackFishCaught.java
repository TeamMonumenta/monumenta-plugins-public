package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class StatTrackFishCaught implements Infusion {
	@Override public ItemStatUtils.InfusionType getInfusionType() {
		return ItemStatUtils.InfusionType.STAT_TRACK_FISH_CAUGHT;
	}

	@Override public String getName() {
		return "Fish Caught";
	}


	public static void fishCaught(Player player) {
		ItemStack is = player.getInventory().getItemInMainHand();
		StatTrackManager.incrementStat(is, player, ItemStatUtils.InfusionType.STAT_TRACK_FISH_CAUGHT, 1);
	}
}
