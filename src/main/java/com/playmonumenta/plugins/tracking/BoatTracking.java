package com.playmonumenta.plugins.tracking;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.LocationUtils;

public class BoatTracking implements EntityTracking {
	Plugin mPlugin = null;
	private Set<Boat> mEntities = new HashSet<Boat>();

	BoatTracking(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public void addEntity(Entity entity) {
		mEntities.add((Boat)entity);
	}

	@Override
	public void removeEntity(Entity entity) {
		mEntities.remove(entity);
	}

	@Override
	public void update(World world, int ticks) {
		Iterator<Boat> boatIter = mEntities.iterator();
		while (boatIter.hasNext()) {
			Boat boat = boatIter.next();
			if (boat != null && boat.isValid()) {
				if (!LocationUtils.isValidBoatLocation(boat.getLocation())) {
					boatIter.remove();
					boat.remove();
				}
			} else {
				boatIter.remove();
			}
		}
	}

	@Override
	public void unloadTrackedEntities() {
		mEntities.clear();
	}
}
