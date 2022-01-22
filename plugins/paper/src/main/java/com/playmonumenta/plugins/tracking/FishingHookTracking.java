package com.playmonumenta.plugins.tracking;

import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;

public class FishingHookTracking implements EntityTracking {
	private HashMap<UUID, FishHook> mEntities = new HashMap<UUID, FishHook>();

	// Note this is not from the base class
	public void addEntity(Player player, Entity entity) {
		mEntities.put(player.getUniqueId(), (FishHook)entity);
	}

	// Note this is not from the base class
	public void removeEntity(Player player) {
		UUID uuid = player.getUniqueId();

		FishHook entity = mEntities.remove(uuid);
		if (entity != null) {
			entity.remove();
		}
	}

	public boolean containsEntity(Player player) {
		return mEntities.get(player.getUniqueId()) != null;
	}

	@Override
	public void unloadTrackedEntities() {
		Iterator<Entry<UUID, FishHook>> iter = mEntities.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<UUID, FishHook> hook = iter.next();
			hook.getValue().remove();
		}

		mEntities.clear();
	}
}
