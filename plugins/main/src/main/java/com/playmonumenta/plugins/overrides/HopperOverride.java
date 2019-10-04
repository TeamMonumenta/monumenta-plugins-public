package com.playmonumenta.plugins.overrides;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;

public class HopperOverride extends BaseOverride {
	@Override
	public boolean blockPlaceInteraction(Plugin plugin, Player player, ItemStack item, BlockPlaceEvent event) {
		if (player != null && !player.getGameMode().equals(GameMode.CREATIVE)) {
			return false;
		}
		return true;
	}

	public boolean blockBreakInteraction(Plugin plugin, Player player, Block block) {
		if (player != null && !player.getGameMode().equals(GameMode.CREATIVE)) {
			return false;
		}
		return true;
	}

	public boolean blockExplodeInteraction(Plugin plugin, Block block) {
		return false;
	}
}
