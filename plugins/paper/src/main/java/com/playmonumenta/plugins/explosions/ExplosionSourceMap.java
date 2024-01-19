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
	private final Map<Location, Double> mExplosionRadii = new HashMap<>();

	public void clear() {
		mExplosionSources.clear();
		mExplosionRadii.clear();
	}

	public void put(Location explosionLocation, Block block) {
		Location location = block.getLocation();
		mExplosionSources
			.computeIfAbsent(location.getWorld().getUID(), k -> new HashMap<>())
			.put(location.toVector(), explosionLocation);

		double radius = location.distance(explosionLocation);
		radius = Double.max(radius, mExplosionRadii.getOrDefault(explosionLocation, 0.0));
		mExplosionRadii.put(explosionLocation, radius);
	}

	public @Nullable Location getSource(Location blockLocation) {
		Map<Vector, Location> worldExplosions = mExplosionSources.get(blockLocation.getWorld().getUID());
		if (worldExplosions == null) {
			return null;
		}
		return worldExplosions.get(blockLocation.toVector());
	}

	public double getRadius(Location explosionSource) {
		return mExplosionRadii.getOrDefault(explosionSource, 0.0);
	}
}
