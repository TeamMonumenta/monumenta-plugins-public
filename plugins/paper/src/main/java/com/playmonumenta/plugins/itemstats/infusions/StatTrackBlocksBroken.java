package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

public class StatTrackBlocksBroken implements Infusion {
	@Override
	public ItemStatUtils.InfusionType getInfusionType() {
		return ItemStatUtils.InfusionType.STAT_TRACK_BLOCKS_BROKEN;
	}

	@Override
	public String getName() {
		return "Blocks Broken";
	}

	@Override
	public void onBlockBreak(Plugin plugin, Player player, double value, BlockBreakEvent event) {
		StatTrackManager.incrementStat(player.getInventory().getItemInMainHand(), player, ItemStatUtils.InfusionType.STAT_TRACK_BLOCKS_BROKEN, 1);
	}
}
