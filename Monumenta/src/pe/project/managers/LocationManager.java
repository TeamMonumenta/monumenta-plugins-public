package pe.project.managers;

import pe.project.locations.safezones.SafeZoneConstants;
import pe.project.locations.safezones.SafeZoneConstants.SafeZones;
import pe.project.point.Point;

public class LocationManager {
	public static SafeZones withinAnySafeZone(Point point) {
		return SafeZoneConstants.withinAnySafeZone(point);
	}
}
