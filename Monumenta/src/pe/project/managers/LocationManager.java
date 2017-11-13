package pe.project.managers;

import org.bukkit.entity.Entity;

import pe.project.locations.safezones.SafeZoneConstants;
import pe.project.locations.safezones.SafeZoneConstants.SafeZones;

public class LocationManager {
	public static SafeZones withinAnySafeZone(Entity entity) {
		return SafeZoneConstants.withinAnySafeZone(entity.getLocation());
	}
}
