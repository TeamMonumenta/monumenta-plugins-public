package com.playmonumenta.plugins.timers;

import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public class ProjectileEffectTimers {
	private final Map<Entity, Particle> mTrackingEntities = new HashMap<>();

	public void addEntity(Entity entity, Particle particle) {
		mTrackingEntities.put(entity, particle);
	}

	public void removeEntity(Entity entity) {
		mTrackingEntities.remove(entity);
	}

	public void update() {
		Iterator<Entry<Entity, Particle>> entityIter = mTrackingEntities.entrySet().iterator();
		while (entityIter.hasNext()) {
			Entry<Entity, Particle> entityHash = entityIter.next();
			Entity entity = entityHash.getKey();
			Particle particle = entityHash.getValue();

			if (entity == null || !entity.isValid() || !entity.isTicking()) {
				entityIter.remove();
				continue;
			}

			// If the projectie lifetime is too long, then there is probably a problem and we should ignore it
			int ticksLived = entity.getTicksLived();
			if (ticksLived >= 200 || (entity instanceof AbstractArrow arrow && arrow.isInBlock())) {
				entityIter.remove();
				continue;
			}

			Vector velocity = entity.getVelocity().clone();
			Location entityLoc = entity.getLocation();
			// Launch particles in the direction of the projectile to prevent particles from blocking the view of the player
			new PartialParticle(particle, entityLoc)
				.directionalMode(true)
				.delta(velocity.getX(), velocity.getY(), velocity.getZ())
				.extra(0.5)
				.distanceFalloff(48)
				.spawnAsEntityActive(entity);
		}
	}
}
