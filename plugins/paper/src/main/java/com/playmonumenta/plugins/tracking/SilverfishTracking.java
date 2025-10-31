package com.playmonumenta.plugins.tracking;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Silverfish;

public class SilverfishTracking implements EntityTracking {
	private final Set<Silverfish> mEntities = Collections.newSetFromMap(new WeakHashMap<>());
	private int mTicks = 0;

	@Override
	public void addEntity(Entity entity) {
		mEntities.add((Silverfish) entity);
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
			if (silverfish != null && silverfish.isValid() && silverfish.getLocation().isChunkLoaded()) {
				if (ZoneUtils.hasZoneProperty(silverfish, ZoneProperty.ADVENTURE_MODE)) {
					// Remove next tick to avoid ConcurrentModificationException
					Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
						silverfish.remove();
					});
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
