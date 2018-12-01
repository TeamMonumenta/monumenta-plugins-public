package com.playmonumenta.plugins.items;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.LocationUtils.LocationType;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;

public class ConduitOverride extends OverrideItem {
	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		if (player == null || player.getGameMode() == GameMode.CREATIVE) {
			return true;
		} else if (player.getGameMode() == GameMode.SURVIVAL && (LocationUtils.getLocationType(plugin, player) == LocationType.Capital)) {
			return true;
		}

		return false;
	}
}
