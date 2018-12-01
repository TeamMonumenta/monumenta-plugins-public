package com.playmonumenta.plugins.items;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.LocationUtils.LocationType;

import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

public class MagmaOverride extends OverrideItem {
	@Override
	public boolean blockPlaceInteraction(Plugin plugin, Player player, ItemStack item, BlockPlaceEvent event) {
		if (player == null || item == null || event == null) {
			return true;
		}

		if (item.hasItemMeta()
		    && item.getItemMeta().hasLore()
		    && player.getGameMode() == GameMode.SURVIVAL) {
			if (LocationUtils.getLocationType(plugin, player) == LocationType.Capital) {
				event.getBlockPlaced().setType(Material.LAVA);
			} else {
				return false;
			}
		}

		return true;
	}
}
