package com.playmonumenta.plugins.managers.travelanchor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class WorldTravelAnchors {
	private final UUID mWorldId;
	private final WorldAnchorGroups mAnchorGroups;
	private final Map<UUID, EntityTravelAnchor> mTravelAnchors = new HashMap<>();

	public WorldTravelAnchors(World world) {
		mWorldId = world.getUID();
		mAnchorGroups = new WorldAnchorGroups(world);
		for (Entity entity : world.getEntities()) {
			if (EntityTravelAnchor.isTravelAnchor(entity)) {
				mTravelAnchors.put(entity.getUniqueId(), EntityTravelAnchor.loadAnchor(entity));
			}
		}
	}

	public @Nullable World getWorld() {
		return Bukkit.getWorld(mWorldId);
	}

	public WorldAnchorGroups getAnchorGroups() {
		return mAnchorGroups;
	}

	public @Nullable EntityTravelAnchor newAnchor(Entity entity) {
		UUID entityId = entity.getUniqueId();
		if (
			!entity.getWorld().getUID().equals(mWorldId)
				|| mTravelAnchors.containsKey(entityId)
				|| EntityTravelAnchor.isTravelAnchor(entity)
		) {
			return null;
		}

		EntityTravelAnchor anchor = EntityTravelAnchor.newAnchor(entity);
		mTravelAnchors.put(entityId, anchor);
		anchor.update();
		return anchor;
	}

	public void loadAnchor(Entity entity) {
		UUID entityId = entity.getUniqueId();
		if (
			!entity.getWorld().getUID().equals(mWorldId)
				|| mTravelAnchors.containsKey(entityId)
				|| !EntityTravelAnchor.isTravelAnchor(entity)
		) {
			return;
		}

		EntityTravelAnchor anchor = EntityTravelAnchor.loadAnchor(entity);
		anchor.update();
		mTravelAnchors.put(entityId, anchor);
	}

	public void removeAnchor(Entity entity) {
		if (mTravelAnchors.remove(entity.getUniqueId()) != null) {
			EntityTravelAnchor.removeAnchor(entity);
		}
	}

	public void unloadAnchor(Entity entity) {
		UUID entityId = entity.getUniqueId();
		mTravelAnchors.remove(entityId);
	}

	public @Nullable EntityTravelAnchor getAnchor(Entity entity) {
		return mTravelAnchors.get(entity.getUniqueId());
	}

	public Collection<EntityTravelAnchor> getAnchors() {
		return new ArrayList<>(mTravelAnchors.values());
	}

	public void updateAnchor(Entity entity) {
		EntityTravelAnchor anchor = mTravelAnchors.get(entity.getUniqueId());
		if (anchor != null) {
			anchor.update();
		}
	}

	public void updateAnchors() {
		mAnchorGroups.save();
		for (EntityTravelAnchor anchor : mTravelAnchors.values()) {
			anchor.update();
		}
	}
}
