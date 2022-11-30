package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class PackedIceOverride extends BaseOverride {
	@Override
	public boolean blockPlaceInteraction(Plugin plugin, Player player, ItemStack item, BlockPlaceEvent event) {
		if (player == null || item == null || event == null) {
			return true;
		}

		if (InventoryUtils.testForItemWithName(item, "Crystallized Water", true)
			    && player.getGameMode() == GameMode.SURVIVAL) {
			if (ZoneUtils.isInPlot(player)) {
				event.getBlockPlaced().setType(Material.WATER);
			} else {
				return false;
			}
		}

		return true;
	}
}
