package pe.project.managers;

import org.bukkit.entity.Entity;

import pe.project.locations.safezones.SafeZoneConstants;
import pe.project.locations.safezones.SafeZoneConstants.SafeZones;
import pe.project.point.Point;

public class LocationManager {
	public static SafeZones withinAnySafeZone(Entity entity) {
		return SafeZoneConstants.withinAnySafeZone(new Point(entity.getLocation()));
	}
}
