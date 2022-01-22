package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

public class StatTrackConsumed implements Infusion {

	@Override
	public String getName() {
		return "Times Consumed";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.STAT_TRACK_CONSUMED;
	}

	@Override
	public void onConsume(Plugin plugin, Player player, double value, PlayerItemConsumeEvent event) {
		if (event.isCancelled()) {
			return;
		}
		ItemStack is = player.getInventory().getItemInMainHand();
		StatTrackManager.incrementStat(is, player, InfusionType.STAT_TRACK_CONSUMED, 1);
	}
}
