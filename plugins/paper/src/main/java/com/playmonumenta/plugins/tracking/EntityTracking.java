package com.playmonumenta.plugins.tracking;

import org.bukkit.entity.Entity;

public interface EntityTracking {
	default void addEntity(Entity entity) {}

	default void removeEntity(Entity entity) {}

	default void unloadTrackedEntities() {}

	default void update(int ticks) {}
}
