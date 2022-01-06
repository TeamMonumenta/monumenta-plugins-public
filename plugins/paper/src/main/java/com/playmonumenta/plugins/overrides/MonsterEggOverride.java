package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.listeners.EntityListener;
import com.playmonumenta.plugins.utils.ZoneUtils;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MonsterEggOverride extends BaseOverride {
	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, @Nullable Block block) {
		// Only allow creative players or players in their plots (in capital and survival) to use spawn eggs
		if ((player.getGameMode() == GameMode.CREATIVE)) {
			return true;
		}
		if (ZoneUtils.isInPlot(player)) {
			return EntityListener.maySummonPlotAnimal(player.getLocation());
		}
		return false;
	}

	@Override
	public boolean rightClickEntityInteraction(Plugin plugin, Player player, Entity clickedEntity,
	                                           ItemStack itemInHand) {
		// Only allow creative players or players in their plots (in capital and survival) to use spawn eggs
		if ((player.getGameMode() == GameMode.CREATIVE)) {
			return true;
		}
		if (ZoneUtils.isInPlot(player)) {
			return EntityListener.maySummonPlotAnimal(player.getLocation());
		}
		return false;
	}

	@Override
	public boolean blockDispenseInteraction(Plugin plugin, Block block, ItemStack dispensed) {
		Material blockType = (block != null) ? block.getType() : Material.AIR;
		if (blockType.equals(Material.DISPENSER)) {
			if (ZoneUtils.isInPlot(block.getLocation())) {
				return EntityListener.maySummonPlotAnimal(block.getLocation());
			}
		}

		return true;
	}
}
