package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;

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
		if (event.isCancelled()
			    // Rage of the Keter cancels the consume event, but should still be counted
			    && ItemStatUtils.getEnchantmentLevel(event.getItem(), ItemStatUtils.EnchantmentType.RAGE_OF_THE_KETER) == 0) {
			return;
		}
		StatTrackManager.incrementStat(event.getItem(), player, InfusionType.STAT_TRACK_CONSUMED, 1);
	}
}
