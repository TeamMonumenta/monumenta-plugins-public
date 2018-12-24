package com.playmonumenta.plugins.tracking;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;

public class CreeperTracking implements EntityTracking {
	private Set<Creeper> mEntities = new HashSet<Creeper>();

	@Override
	public void addEntity(Entity entity) {
		mEntities.add((Creeper)entity);
	}

	@Override
	public void removeEntity(Entity entity) {
		mEntities.remove(entity);
	}

	@Override
	public void update(World world, int ticks) {
		Iterator<Creeper> creeperIter = mEntities.iterator();
		while (creeperIter.hasNext()) {
			Creeper creeper = creeperIter.next();
			if (creeper != null && creeper.isValid()) {
				Set<String> tags = creeper.getScoreboardTags();
				if (tags != null && tags.contains("Snuggles")) {
					world.spawnParticle(Particle.HEART, creeper.getLocation().add(0, 1, 0), 1, 0.4, 1, 0.4, 0);
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
