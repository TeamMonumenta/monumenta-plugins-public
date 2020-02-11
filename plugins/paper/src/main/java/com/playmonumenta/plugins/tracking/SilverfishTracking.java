package com.playmonumenta.plugins.tracking;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Silverfish;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

public class SilverfishTracking implements EntityTracking {
	Plugin mPlugin = null;
	private Set<Silverfish> mEntities = new HashSet<Silverfish>();

	SilverfishTracking(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public void addEntity(Entity entity) {
		mEntities.add((Silverfish)entity);
	}

	@Override
	public void removeEntity(Entity entity) {
		mEntities.remove(entity);
	}

	@Override
	public void update(World world, int ticks) {
		Iterator<Silverfish> silverfishIter = mEntities.iterator();
		while (silverfishIter.hasNext()) {
			Silverfish silverfish = silverfishIter.next();
			if (silverfish != null && silverfish.isValid()) {
				if (ZoneUtils.hasZoneProperty(silverfish, ZoneProperty.ADVENTURE_MODE)) {
					silverfish.remove();
					silverfishIter.remove();
				}
			} else {
				silverfishIter.remove();
			}
		}
	}

	@Override
	public void unloadTrackedEntities() {
		mEntities.clear();
	}
}
