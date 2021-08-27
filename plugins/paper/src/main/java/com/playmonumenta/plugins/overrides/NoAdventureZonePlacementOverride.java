package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class NoAdventureZonePlacementOverride extends BaseOverride {
	@Override
	public boolean blockPlaceInteraction(Plugin plugin, Player player, ItemStack item, BlockPlaceEvent event) {
		if (player != null && !player.getGameMode().equals(GameMode.CREATIVE) && ZoneUtils.hasZoneProperty(event.getBlock().getLocation(), ZoneProperty.ADVENTURE_MODE)) {
			return false;
		}
		return true;
	}

	public boolean blockBreakInteraction(Plugin plugin, Player player, Block block) {
		if (player != null && !player.getGameMode().equals(GameMode.CREATIVE) && ZoneUtils.hasZoneProperty(block.getLocation(), ZoneProperty.ADVENTURE_MODE)) {
			return false;
		}
		return true;
	}

	public boolean blockExplodeInteraction(Plugin plugin, Block block) {
		if (ZoneUtils.hasZoneProperty(block.getLocation(), ZoneProperty.ADVENTURE_MODE)) {
			return false;
		}
		return true;
	}
}
