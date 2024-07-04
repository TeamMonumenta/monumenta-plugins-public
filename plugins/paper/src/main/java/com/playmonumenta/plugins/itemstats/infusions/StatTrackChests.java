package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class StatTrackChests implements Infusion {

	@Override
	public String getName() {
		return "Chests Broken";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.STAT_TRACK_CHEST_BROKEN;
	}

	@Override
	public void onBlockBreak(Plugin plugin, Player player, double value, BlockBreakEvent event) {
		if (event.getBlock().getType() != Material.CHEST) {
			return;
		}

		ItemStack is = player.getInventory().getItemInMainHand();
		//We killed a chest, so increase the stat
		StatTrackManager.incrementStat(is, player, InfusionType.STAT_TRACK_CHEST_BROKEN, 1);
	}
}
