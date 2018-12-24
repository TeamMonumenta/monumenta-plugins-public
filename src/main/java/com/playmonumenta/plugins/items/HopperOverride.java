package com.playmonumenta.plugins.items;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;

public class HopperOverride extends OverrideItem {
	@Override
	public boolean blockPlaceInteraction(Plugin plugin, Player player, ItemStack item, BlockPlaceEvent event) {
		if (player != null && player.getGameMode() != GameMode.CREATIVE) {
			return false;
		}
		return true;
	}
}
