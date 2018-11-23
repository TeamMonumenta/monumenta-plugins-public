package com.playmonumenta.plugins.items;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.LocationUtils.LocationType;

import org.bukkit.block.Block;

import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;

public class EnderChestOverride extends OverrideItem {
	@Override
	public boolean blockPlaceInteraction(Plugin plugin, Player player, ItemStack item, BlockPlaceEvent event) {
		if (player == null || player.getGameMode() == GameMode.CREATIVE) {
			return true;
		} else if (player.getGameMode() == GameMode.SURVIVAL && (LocationUtils.getLocationType(plugin, player) == LocationType.Capital)) {
			return true;
		}

		return false;
	}

	/* Chests placed on barriers can not be broken */
	@Override
	public boolean blockBreakInteraction(Plugin plugin, Player player, Block block) {
		if ((player.getGameMode() == GameMode.CREATIVE) || ChestOverride._breakable(block)) {
			return true;
		}
		return false;
	}
}
