package com.playmonumenta.plugins.overrides;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.safezone.SafeZoneManager.LocationType;

public class MagmaOverride extends BaseOverride {
	@Override
	public boolean blockPlaceInteraction(Plugin plugin, Player player, ItemStack item, BlockPlaceEvent event) {
		if (player == null || item == null || event == null) {
			return true;
		}

		if (item.hasItemMeta()
		    && item.getItemMeta().hasLore()
		    && player.getGameMode() == GameMode.SURVIVAL) {
			if (plugin.mSafeZoneManager.getLocationType(player) == LocationType.Capital) {
				event.getBlockPlaced().setType(Material.LAVA);
			} else {
				return false;
			}
		}

		return true;
	}
}
