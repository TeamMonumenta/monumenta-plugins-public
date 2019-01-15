package com.playmonumenta.plugins.tracking;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Entity;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.LocationUtils;

public class MinecartTracking implements EntityTracking {
	Plugin mPlugin = null;
	private Set<Minecart> mEntities = new HashSet<Minecart>();

	MinecartTracking(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public void addEntity(Entity entity) {
		mEntities.add((Minecart)entity);
	}

	@Override
	public void removeEntity(Entity entity) {
		mEntities.remove(entity);
	}

	@Override
	public void update(World world, int ticks) {
		Iterator<Minecart> minecartIter = mEntities.iterator();
		while (minecartIter.hasNext()) {
			Minecart minecart = minecartIter.next();
			if (minecart != null && minecart.isValid()) {
				if (!LocationUtils.isValidMinecartLocation(minecart.getLocation())) {
					minecartIter.remove();
					minecart.remove();
				}
			} else {
				minecartIter.remove();
			}
		}
	}

	@Override
	public void unloadTrackedEntities() {
		mEntities.clear();
	}
}
