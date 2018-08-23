package com.playmonumenta.plugins.timers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;

import com.playmonumenta.plugins.utils.EntityUtils;

import java.util.UUID;

//	TODO - Expand this to take in a "Timer Type" id as well so we can store multiple different types of
//	timers for players in the future with this.
public class CombatLoggingTimers {
	public HashMap<UUID, Integer> mTimers = null;

	public CombatLoggingTimers() {
		mTimers = new HashMap<UUID, Integer>();
	}

	public void addTimer(UUID entityUUID, int time) {
		mTimers.put(entityUUID, time);
	}

	public Integer getTimer(UUID entityUUID) {
		if (mTimers.containsKey(entityUUID)) {
			return mTimers.get(entityUUID);
		}

		return -1;
	}

	public void removeTimer(UUID entityUUID) {
		mTimers.remove(entityUUID);
	}

	public void update(World world, Integer ticks) {
		Iterator<Entry<UUID, Integer>> iter = mTimers.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<UUID, Integer> entityEntry = iter.next();

			int oldTimer = entityEntry.getValue();
			if (oldTimer > 0) {
				int newTimer = oldTimer - ticks;
				if (newTimer > 0) {
					entityEntry.setValue(newTimer);
				} else {
					Entity entity = EntityUtils.getEntity(world, entityEntry.getKey());
					if (entity != null) {
						if (entity instanceof Monster) {
							Monster mob = (Monster)entity;

							Set<String> tags = mob.getScoreboardTags();
							if (!tags.contains("Elite") && !tags.contains("Boss")) {
								mob.setRemoveWhenFarAway(true);
							}

							iter.remove();
						}
					} else {
						entityEntry.setValue(0);
					}
				}
			}
		}
	}
}
