package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class StatTrackSpawners implements Infusion {

	@Override
	public String getName() {
		return "Spawners Broken";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.STAT_TRACK_SPAWNER;
	}


	@Override
	public void onBlockBreak(Plugin plugin, Player player, double value, BlockBreakEvent event) {

		if (event.getBlock().getType() != Material.SPAWNER) {
			return;
		}

		ItemStack is = player.getInventory().getItemInMainHand();
		//We killed a spawner, so increase the stat
		StatTrackManager.incrementStat(is, player, InfusionType.STAT_TRACK_SPAWNER, 1);
	}
}
