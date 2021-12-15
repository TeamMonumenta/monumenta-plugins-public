package com.playmonumenta.plugins.timers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;

import com.playmonumenta.plugins.utils.EntityUtils;

public class CombatLoggingTimers {

	private final HashMap<UUID, Integer> mTimers = new HashMap<>();

	public CombatLoggingTimers() {
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

	public void update(Integer ticks) {
		Iterator<Entry<UUID, Integer>> iter = mTimers.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<UUID, Integer> entityEntry = iter.next();

			int oldTimer = entityEntry.getValue();
			if (oldTimer > 0) {
				int newTimer = oldTimer - ticks;
				if (newTimer > 0) {
					entityEntry.setValue(newTimer);
				} else {
					for (World world : Bukkit.getWorlds()) {
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
}
