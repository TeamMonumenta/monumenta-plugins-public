package pe.project.tracking;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;

import pe.project.locations.safezones.SafeZoneConstants.SafeZones;
import pe.project.managers.LocationManager;
import pe.project.point.Point;

public class BoatTracking implements EntityTracking {

	@Override
	public void addEntity(Entity entity) {
		mEntities.add((Boat)entity);
	}
	
	@Override
	public void removeEntity(Entity entity) {
		mEntities.remove(entity);
	}

	@Override
	public void update(World world) {
		Iterator<Boat> boatIter = mEntities.iterator();
		while (boatIter.hasNext()) {
			Boat boat = boatIter.next();
			if (boat != null && boat.isValid()) {
				Point loc = new Point(boat.getLocation());
				SafeZones safeZone = LocationManager.withinAnySafeZone(loc);
				if (safeZone != SafeZones.None) {
					boatIter.remove();
					boat.remove();
				}
			} else {
				boatIter.remove();
			}
		}
	}

	private Set<Boat> mEntities = new HashSet<Boat>();
}
