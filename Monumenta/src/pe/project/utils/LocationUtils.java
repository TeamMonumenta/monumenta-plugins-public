package pe.project.utils;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import pe.project.Plugin;
import pe.project.point.AreaBounds;
import pe.project.point.Point;

public class LocationUtils {
	public enum LocationType {
		None(-1),

		// Adventure + Resistance + Speed
		Capital(0),

		// Adventure + Resistance
		SafeZone(1),

		// Adventure
		AdventureZone(2),

		// Restricted - Adventure and some interactions are restricted
		RestrictedZone(3);

		public int mValue;
		private LocationType(int value) {
			this.mValue = value;
		}
	}

	public static LocationType getLocationType(Plugin plugin, Entity entity) {
		return getLocationType(plugin, entity.getLocation());
	}

	public static LocationType getLocationType(Plugin plugin, Location location) {
		return getLocationType(plugin, new Point(location));
	}

	public static LocationType getLocationType(Plugin plugin, Point point) {
		if (plugin.mServerProporties.getIsTownWorld()) {
			return LocationType.Capital;
		}

		for (AreaBounds area : plugin.mServerProporties.mLocationBounds) {
			if (area.within(point)) {
				return area.getType();
			}
		}

		return LocationType.None;
	}
}
