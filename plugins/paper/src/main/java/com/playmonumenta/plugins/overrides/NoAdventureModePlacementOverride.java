package com.playmonumenta.plugins.overrides;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;

public class NoAdventureModePlacementOverride extends BaseOverride {
	@Override
	public boolean blockPlaceInteraction(Plugin plugin, Player player, ItemStack item, BlockPlaceEvent event) {
		if (player != null && player.getGameMode().equals(GameMode.ADVENTURE)) {
			return false;
		}
		return true;
	}
}
