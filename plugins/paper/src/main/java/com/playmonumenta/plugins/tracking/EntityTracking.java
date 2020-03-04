package com.playmonumenta.plugins.tracking;

import org.bukkit.World;
import org.bukkit.entity.Entity;

public interface EntityTracking {
	void addEntity(Entity entity);

	void removeEntity(Entity entity);

	void unloadTrackedEntities();

	void update(World world, int ticks);
}
