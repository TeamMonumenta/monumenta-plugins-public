package pe.project.items;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import pe.project.Plugin;
import pe.project.utils.LocationUtils;
import pe.project.utils.LocationUtils.LocationType;

public class PackedIceOverride extends OverrideItem {
	@Override
	public boolean blockPlaceInteraction(Plugin plugin, Player player, ItemStack item, BlockPlaceEvent event) {
		if (player == null || item == null || event == null) {
			return true;
		}

		if (item.hasItemMeta()
				&& item.getItemMeta().hasLore()
				&& player.getGameMode() == GameMode.SURVIVAL
				&& LocationUtils.getLocationType(plugin, player) == LocationType.Capital) {
			event.getBlockPlaced().setType(Material.STATIONARY_WATER);
		}

		return true;
	}
}
