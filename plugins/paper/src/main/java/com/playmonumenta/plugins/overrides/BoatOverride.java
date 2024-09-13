package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class BoatOverride extends BaseOverride {
	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, @Nullable Block block) {
		if (player == null || player.getGameMode() == GameMode.CREATIVE) {
			return true;
		}

		// Ignore the passed-in block and compute which block the player is looking at
		block = player.getTargetBlockExact(6, FluidCollisionMode.SOURCE_ONLY);
		if (block == null) {
			return false;
		}

		Location loc = block.getLocation();
		if (ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.NO_PLACING_BOATS)
				|| ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.NO_PLACING_BOATS)) {
			return false;
		}

		// Must place boats on water or ice
		if (!LocationUtils.isValidBoatLocation(loc)) {
			return false;
		}

		// Must not spam boats
		return !tooManyBoatsNearby(loc);
	}

	@Override
	public boolean blockDispenseInteraction(Plugin plugin, Block block, ItemStack dispensed) {
		Location loc = block.getLocation();
		if (ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.NO_PLACING_BOATS)) {
			return false;
		}
		return !tooManyBoatsNearby(loc);
	}

	private boolean tooManyBoatsNearby(Location loc) {
		return loc.getWorld().getNearbyEntitiesByType(Boat.class, loc, 10).size() > 7;
	}
}
