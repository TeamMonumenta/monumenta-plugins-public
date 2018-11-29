package com.playmonumenta.plugins.items;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.LocationUtils.LocationType;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Cow;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.World;

import com.playmonumenta.plugins.Plugin;

public class BucketOverride extends OverrideItem {
	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		if (player == null || player.getGameMode() == GameMode.CREATIVE) {
			return true;
		} else if (player.getGameMode() == GameMode.SURVIVAL && (LocationUtils.getLocationType(plugin, player) == LocationType.Capital)) {
			return true;
		}

		return false;
	}

	public boolean rightClickEntityInteraction(Plugin plugin, Player player, Entity clickedEntity,
	                                           ItemStack itemInHand) {
		if (clickedEntity == null) {
			return true;
		} else if (clickedEntity instanceof Cow) {
			return false;
		}

		return true;
	}

	public boolean blockDispenseInteraction(Plugin plugin, Block block, ItemStack dispensed) {
		Material blockType = (block != null) ? block.getType() : Material.AIR;
		if ( blockType.equals(Material.AIR) || dispensed == null ) {
			return false;
		} else if (blockType.equals(Material.DISPENSER)) {
			if (LocationUtils.isInPlot(plugin, block.getWorld(), block.getLocation())) {
				return true;
			} else {
				return false;
			}
		} else if (blockType.equals(Material.DROPPER)) {
			return true;
		}

		return false;
	}
}
