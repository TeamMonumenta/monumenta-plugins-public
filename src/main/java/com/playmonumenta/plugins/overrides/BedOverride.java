package com.playmonumenta.plugins.overrides;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.safezone.SafeZoneManager.LocationType;

public class BedOverride extends BaseOverride {
	@Override
	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		if (item == null || item.getType().equals(Material.AIR)) {
			LocationType zone = plugin.mSafeZoneManager.getLocationType(player);
			if (zone == LocationType.Capital || zone == LocationType.SafeZone) {
				Location loc = block.getLocation().add(0.5, -1.2, 0.5);
				Location pLoc = player.getLocation();
				Vector dir = pLoc.getDirection().setY(0).normalize();
				loc.setDirection(dir.multiply(-1));
				StairsOverride.sitOnLocation(plugin, player, loc, block);
				return false;
			}
			return true;
		} else {
			return plugin.mServerProperties.getIsSleepingEnabled();
		}
	}
}
