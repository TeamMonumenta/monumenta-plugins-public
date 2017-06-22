package pe.project.managers;

import pe.project.locations.cities.CityConstants;
import pe.project.locations.cities.CityConstants.Cities;
import pe.project.point.Point;

public class LocationManager {
	public static Cities WithinSafeZone(Point point) {
		return CityConstants.withinSafeZone(point);
	}
}
