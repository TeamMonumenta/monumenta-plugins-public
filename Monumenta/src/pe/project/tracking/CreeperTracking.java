package pe.project.tracking;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;

import pe.project.utils.ParticleUtils;
import pe.project.locations.safezones.SafeZoneConstants.SafeZones;
import pe.project.managers.LocationManager;
import pe.project.point.Point;

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
	public void update(World world) {
		Iterator<Creeper> creeperIter = mEntities.iterator();
		while (creeperIter.hasNext()) {
			Creeper creeper = creeperIter.next();
			if (creeper != null && creeper.isValid()) {
				Point loc = new Point(creeper.getLocation());
				boolean creeperRemoved = false;
				Set<String> tags = creeper.getScoreboardTags();
				boolean snuggles = false;
				if (tags != null) {
					snuggles = tags.contains("Snuggles");
				}
				
				SafeZones safeZone = LocationManager.withinAnySafeZone(loc);
				if (safeZone != SafeZones.None) {
					if (safeZone == SafeZones.Farr) {
						if (!snuggles) {
							creeperIter.remove();
							creeper.remove();
							creeperRemoved = true;
						}
					} else {
						creeperIter.remove();
						creeper.remove();
						creeperRemoved = true;
					}
				}
				
				if (!creeperRemoved && snuggles) {
					_updateSnugglesParticles(creeper, world);
				}
			} else {
				creeperIter.remove();
			}
		}
	}
	
	private void _updateSnugglesParticles(Creeper creeper, World world) {
		ParticleUtils.playParticlesInWorld(world, Particle.HEART, creeper.getLocation().add(0, 1, 0), 1, 0.4, 1, 0.4, 0);
	}
}
