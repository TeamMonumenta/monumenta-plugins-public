package pe.project.classes.Timers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import pe.project.classes.Utils.ParticleUtil;

public class ProjectileEffectTimers {
	private World mWorld;
	private HashMap<Entity, Particle> mTrackingEntities;
	
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
		Iterator<Entry<Entity, Particle>> entityIter = mTrackingEntities.entrySet().iterator();
		while (entityIter.hasNext()) {
			Entry<Entity, Particle> entityHash = entityIter.next();
			Entity entity = entityHash.getKey();
			Particle particle = entityHash.getValue();
			
			int numParticles = 3;
			
			//	Because some particles are big
			if (particle == Particle.CLOUD) {
				numParticles = 1;
			}
			
			ParticleUtil.playParticlesInWorld(mWorld, particle, entity.getLocation(), numParticles, 0.1, 0.1, 0.1, 0);
		}
	}
}
