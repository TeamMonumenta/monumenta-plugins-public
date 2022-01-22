package com.playmonumenta.plugins.tracking;

import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Particle;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class CreeperTracking implements EntityTracking {
	private Set<Creeper> mEntities = new HashSet<Creeper>();
	private int mTicks = 0;

	@Override
	public void addEntity(Entity entity) {
		mEntities.add((Creeper)entity);
	}

	@Override
	public void removeEntity(Entity entity) {
		mEntities.remove(entity);
	}

	@Override
	public void update(int ticks) {
		Iterator<Creeper> creeperIter = mEntities.iterator();
		while (creeperIter.hasNext()) {
			Creeper creeper = creeperIter.next();
			if (creeper != null && creeper.isValid() && creeper.getLocation().isChunkLoaded()) {
				Set<String> tags = creeper.getScoreboardTags();
				if (tags != null && tags.contains("Snuggles")) {
					creeper.getWorld().spawnParticle(Particle.HEART, creeper.getLocation().add(0, 1, 0), 1, 0.4, 1, 0.4, 0);
				}

				// Very infrequently check if the creeper is still actually there
				mTicks++;
				if (mTicks > 306) {
					mTicks = 0;
					if (!EntityUtils.isStillLoaded(creeper)) {
						creeperIter.remove();
					}
				}
			} else {
				creeperIter.remove();
			}
		}
	}

	@Override
	public void unloadTrackedEntities() {
		mEntities.clear();
	}
}
