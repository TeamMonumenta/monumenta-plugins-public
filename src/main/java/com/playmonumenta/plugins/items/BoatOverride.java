package com.playmonumenta.plugins.items;

import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.LocationUtils;

public class BoatOverride extends OverrideItem {
	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		if (player == null) {
			return true;
		}

		// Ignore the passed-in block and compute which block the player is looking at
		block = player.getTargetBlockExact(6, FluidCollisionMode.SOURCE_ONLY);
		if (block == null) {
			return false;
		}

		return LocationUtils.isValidBoatLocation(block.getLocation());
	}
}
