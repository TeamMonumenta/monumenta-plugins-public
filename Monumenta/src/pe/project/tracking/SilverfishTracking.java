package pe.project.tracking;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Silverfish;

import pe.project.locations.safezones.SafeZoneConstants.SafeZones;
import pe.project.managers.LocationManager;
import pe.project.point.Point;

public class SilverfishTracking implements EntityTracking {
	@Override
	public void addEntity(Entity entity) {
		mEntities.add((Silverfish)entity);
	}
	
	@Override
	public void removeEntity(Entity entity) {
		mEntities.remove(entity);
	}

	@Override
	public void update(World world) {
		Iterator<Silverfish> silverfishIter = mEntities.iterator();
		while (silverfishIter.hasNext()) {
			Silverfish silverfish = silverfishIter.next();
			if (silverfish != null && silverfish.isValid()) {
				Point loc = new Point(silverfish.getLocation());
				SafeZones safeZone = LocationManager.withinAnySafeZone(loc);
				if (safeZone != SafeZones.None) {
					silverfish.remove();
					silverfishIter.remove();
				}
			} else {
				silverfishIter.remove();
			}
		}
	}

	private Set<Silverfish> mEntities = new HashSet<Silverfish>();
}
