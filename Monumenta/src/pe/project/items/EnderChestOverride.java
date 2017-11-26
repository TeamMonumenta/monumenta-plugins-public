package pe.project.items;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import pe.project.Plugin;
import pe.project.managers.LocationUtils;
import pe.project.managers.LocationUtils.LocationType;

public class EnderChestOverride extends OverrideItem {
	@Override
	public boolean blockPlaceInteraction(Plugin plugin, Player player, ItemStack item, BlockPlaceEvent event) {
		if (player == null || player.getGameMode() == GameMode.CREATIVE) {
			return true;
		} else if (player.getGameMode() == GameMode.SURVIVAL && (LocationUtils.getLocationType(plugin, player) == LocationType.Capital)) {
			return true;
		}

		return false;
	}
}
