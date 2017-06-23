package pe.project.tracking;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TNTPrimed;

import pe.project.locations.safezones.SafeZoneConstants.SafeZones;
import pe.project.managers.LocationManager;
import pe.project.point.Point;

public class TNTTracking implements EntityTracking {

	@Override
	public void addEntity(Entity entity) {
		mEntities.add((TNTPrimed)entity);
	}
	
	@Override
	public void removeEntity(Entity entity) {
		mEntities.remove(entity);
	}

	@Override
	public void update(World world) {
		Iterator<TNTPrimed> tntIter = mEntities.iterator();
		while (tntIter.hasNext()) {
			TNTPrimed tnt = tntIter.next();
			if (tnt != null && tnt.isValid()) {
				Point loc = new Point(tnt.getLocation());
				SafeZones city = LocationManager.WithinSafeZone(loc);
				if (city != SafeZones.None) {
					tnt.remove();
					tntIter.remove();
				}
			} else {
				tntIter.remove();
			}
		}
	}

	private Set<TNTPrimed> mEntities = new HashSet<TNTPrimed>();
}
