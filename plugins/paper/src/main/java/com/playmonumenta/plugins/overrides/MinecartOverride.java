package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MinecartOverride extends BaseOverride {
	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, @Nullable Block block) {
		if (player == null) {
			return true;
		}

		// Ignore the passed-in block and compute which block the player is looking at
		Block targetBlock = player.getTargetBlockExact(6, FluidCollisionMode.SOURCE_ONLY);
		if (targetBlock == null) {
			return false;
		}

		return LocationUtils.isValidMinecartLocation(targetBlock.getLocation());
	}
}
