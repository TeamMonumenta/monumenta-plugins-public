package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRiptideEvent;

public class StatTrackRiptide implements Infusion {

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.STAT_TRACK_RIPTIDE;
	}

	@Override
	public String getName() {
		return "Times Riptided";
	}

	@Override
	public void onRiptide(Plugin plugin, Player player, double value, PlayerRiptideEvent event) {
		StatTrackManager.incrementStat(player.getInventory().getItemInMainHand(), player, InfusionType.STAT_TRACK_RIPTIDE, 1);
	}
}
