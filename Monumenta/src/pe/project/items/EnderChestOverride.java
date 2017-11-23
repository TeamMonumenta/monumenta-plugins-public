package pe.project.items;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import pe.project.Plugin;
import pe.project.locations.safezones.SafeZoneConstants;
import pe.project.locations.safezones.SafeZoneConstants.SafeZones;

public class EnderChestOverride extends OverrideItem {
	@Override
	public boolean blockPlaceInteraction(Plugin plugin, Player player, ItemStack item, BlockPlaceEvent event) {
		if (player == null || player.getGameMode() == GameMode.CREATIVE) {
			return true;
		} else if (player.getGameMode() == GameMode.SURVIVAL
				&& ((SafeZoneConstants.withinAnySafeZone(player.getLocation()) != SafeZones.None) || plugin.mServerProporties.getIsTownWorld())) {
			return true;
		}

		return false;
	}
}
