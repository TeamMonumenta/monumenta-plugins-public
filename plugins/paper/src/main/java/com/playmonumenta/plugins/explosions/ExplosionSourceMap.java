package com.playmonumenta.plugins.explosions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ExplosionSourceMap {
	private final Map<UUID, Map<Vector, Location>> mExplosionSources = new HashMap<>();

	public void clear() {
		mExplosionSources.clear();
	}

	public void put(Location explosionLocation, Block block) {
		Location location = block.getLocation();
		Map<Vector, Location> worldExplosions
			= mExplosionSources.computeIfAbsent(location.getWorld().getUID(), k -> new HashMap<>());
		worldExplosions.put(location.toVector(), explosionLocation);
	}

	public @Nullable Location get(Location blockLocation) {
		Map<Vector, Location> worldExplosions = mExplosionSources.get(blockLocation.getWorld().getUID());
		if (worldExplosions == null) {
			return null;
		}
		return worldExplosions.get(blockLocation.toVector());
	}
}
