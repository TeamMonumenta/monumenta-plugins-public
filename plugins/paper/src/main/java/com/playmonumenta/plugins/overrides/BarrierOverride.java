package com.playmonumenta.plugins.overrides;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

public class BarrierOverride extends BaseOverride {
	@Override
	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block, PlayerInteractEvent event) {
		if ((block.getType() == Material.BARRIER) && (ZoneUtils.hasZoneProperty(block.getLocation(), ZoneProperty.PLOTS_POSSIBLE)) && (player.getGameMode() != GameMode.CREATIVE) && (event.getBlockFace() == BlockFace.UP)) {
			return false;
		}
		return true;
	}
}
