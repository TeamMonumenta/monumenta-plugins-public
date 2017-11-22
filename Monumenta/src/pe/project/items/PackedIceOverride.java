package pe.project.items;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import pe.project.Plugin;
import pe.project.locations.safezones.SafeZoneConstants;
import pe.project.locations.safezones.SafeZoneConstants.SafeZones;

public class PackedIceOverride extends OverrideItem {
	@Override
	public boolean blockPlaceInteraction(Plugin plugin, Player player, ItemStack item, BlockPlaceEvent event) {
		if (item.hasItemMeta() && item.getItemMeta().hasLore() && player.getGameMode() == GameMode.SURVIVAL
				&& ((SafeZoneConstants.withinAnySafeZone(player.getLocation()) != SafeZones.None) || plugin.mServerProporties.getIsTownWorld())) {
			event.getBlockPlaced().setType(Material.STATIONARY_WATER);
		}

		return true;
	}
}
