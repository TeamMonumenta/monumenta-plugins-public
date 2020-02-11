package com.playmonumenta.plugins.overrides;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

public class MagmaOverride extends BaseOverride {
	@Override
	public boolean blockPlaceInteraction(Plugin plugin, Player player, ItemStack item, BlockPlaceEvent event) {
		if (player == null || item == null || event == null) {
			return true;
		}

		if (InventoryUtils.testForItemWithLore(item, "Turns into lava when")
		    && player.getGameMode() == GameMode.SURVIVAL) {
			if (ZoneUtils.hasZoneProperty(player, ZoneProperty.PLOTS_POSSIBLE)) {
				event.getBlockPlaced().setType(Material.LAVA);
			} else {
				return false;
			}
		}

		return true;
	}
}
