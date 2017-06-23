package pe.project.managers;

import pe.project.locations.cities.CityConstants;
import pe.project.locations.cities.CityConstants.SafeZones;
import pe.project.point.Point;

public class LocationManager {
	public static SafeZones WithinSafeZone(Point point) {
		return CityConstants.withinAnySafeZone(point);
	}
}
