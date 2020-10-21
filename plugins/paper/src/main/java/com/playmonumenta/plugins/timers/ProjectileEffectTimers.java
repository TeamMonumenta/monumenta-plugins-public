package com.playmonumenta.plugins.timers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;

public class ProjectileEffectTimers {
	private Plugin mPlugin;
	private HashMap<Entity, Particle> mTrackingEntities;
	private int mDeadTicks = 0;

	public ProjectileEffectTimers(Plugin plugin) {
		mPlugin = plugin;
		mTrackingEntities = new HashMap<Entity, Particle>();
	}

	public void addEntity(Entity entity, Particle particle) {
		// 2 tick delay so particles don't get spammed in the player's face
		new BukkitRunnable() {
			@Override
			public void run() {
				// Make sure the entity still exists and has not yet landed
				if (entity.isValid() && !(entity instanceof AbstractArrow && ((AbstractArrow) entity).isInBlock())) {
					mTrackingEntities.put(entity, particle);
				}
			}
		}.runTaskLater(mPlugin, 2);
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

			Location entityLoc = entity.getLocation();
			entityLoc.getWorld().spawnParticle(particle, entityLoc, numParticles, 0.1, 0.1, 0.1, 0);

			/* EVery so often check if this entity is actually still there */
			if (mDeadTicks > 100) {
				mDeadTicks = 0;
				boolean isPresent = false;
				for (Entity e : entityLoc.getWorld().getNearbyEntities(entityLoc, 4, 4, 4)) {
					if (e.getUniqueId().equals(entity.getUniqueId())) {
						isPresent = true;
					}
				}
				if (entity.isDead() || !entity.isValid()) {
					isPresent = false;
				}

				if (!isPresent) {
					entityIter.remove();
				}
			}
		}
	}
}
