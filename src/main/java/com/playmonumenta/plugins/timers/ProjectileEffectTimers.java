package com.playmonumenta.plugins.timers;

import com.playmonumenta.plugins.utils.ParticleUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.bukkit.entity.Entity;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

public class ProjectileEffectTimers {
	private World mWorld;
	private HashMap<Entity, Particle> mTrackingEntities;
	private int mDeadTicks = 0;

	public ProjectileEffectTimers(World world) {
		mWorld = world;
		mTrackingEntities = new HashMap<Entity, Particle>();
	}

	public void addEntity(Entity entity, Particle particle) {
		mTrackingEntities.put(entity, particle);
	}

	public void removeEntity(Entity entity) {
		mTrackingEntities.remove(entity);
	}

	public void update() {
		mDeadTicks++;

		Iterator<Entry<Entity, Particle>> entityIter = mTrackingEntities.entrySet().iterator();
		while (entityIter.hasNext()) {
			Entry<Entity, Particle> entityHash = entityIter.next();
			Entity entity = entityHash.getKey();
			Particle particle = entityHash.getValue();

			int numParticles = 3;

			//  Because some particles are big
			if (particle == Particle.CLOUD) {
				numParticles = 1;
			}

			mWorld.spawnParticle(particle, entity.getLocation(), numParticles, 0.1, 0.1, 0.1, 0);

			/* EVery so often check if this entity is actually still there */
			if (mDeadTicks > 100) {
				mDeadTicks = 0;
				boolean isPresent = false;
				Location entityLoc = entity.getLocation();
				for (Entity e : entityLoc.getWorld().getNearbyEntities(entityLoc, 4, 4, 4)) {
					if (e.getUniqueId().equals(entity.getUniqueId())) {
						isPresent = true;
					}
				}

				if (!isPresent) {
					entityIter.remove();
				}
			}
		}
	}
}
