package com.playmonumenta.plugins.tracking;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Silverfish;

import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

public class SilverfishTracking implements EntityTracking {
	private Set<Silverfish> mEntities = new HashSet<Silverfish>();
	private int mTicks = 0;

	@Override
	public void addEntity(Entity entity) {
		mEntities.add((Silverfish)entity);
	}

	@Override
	public void removeEntity(Entity entity) {
		mEntities.remove(entity);
	}

	@Override
	public void update(int ticks) {
		Iterator<Silverfish> silverfishIter = mEntities.iterator();
		while (silverfishIter.hasNext()) {
			Silverfish silverfish = silverfishIter.next();
			if (silverfish != null && silverfish.isValid()) {
				if (ZoneUtils.hasZoneProperty(silverfish, ZoneProperty.ADVENTURE_MODE)) {
					silverfish.remove();
					silverfishIter.remove();
				} else {
					// Very infrequently check if the silverfish is still actually there
					mTicks++;
					if (mTicks > 306) {
						mTicks = 0;
						if (!EntityUtils.isStillLoaded(silverfish)) {
							silverfishIter.remove();
						}
					}
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
