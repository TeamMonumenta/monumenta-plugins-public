package pe.project.managers;

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
		AdventureZone(2);

		public int mValue;
		private LocationType(int value)	{ this.mValue = value; }
	}

	private static final AreaBounds[] mLocationBounds = {
			// Capital
			new AreaBounds("Capital", LocationType.Capital,
					       new Point(-1130, 0,-284), new Point(-498, 256, 344)),

			// SafeZone
			new AreaBounds("Nyr", LocationType.SafeZone,
					       new Point(-181, 0, -166), new Point(-79, 256, 14)),
			new AreaBounds("Farr", LocationType.SafeZone,
					       new Point(538, 0, 100), new Point(658, 256, 229)),
			new AreaBounds("Highwatch", LocationType.SafeZone,
					       new Point(1130, 0, -156), new Point(1242, 256, -76)),
			new AreaBounds("Lowtide Main", LocationType.SafeZone,
					       new Point(675, 0, 421), new Point(767, 255, 558)),
			new AreaBounds("Lowtide docks", LocationType.SafeZone,
					       new Point(664, 0, 474), new Point(675, 255, 483)),
			new AreaBounds("Lowtide boat", LocationType.SafeZone,
					       new Point(650, 0, 483), new Point(675, 255, 558)),

			new AreaBounds("White Wool Lobby", LocationType.SafeZone,
					       new Point(136, 53, -186), new Point(176, 83, -120)),
			new AreaBounds("Orange Wool Lobby", LocationType.SafeZone,
					       new Point(27, 64, 164), new Point(67, 94, 229)),
			new AreaBounds("Magenta Wool Lobby", LocationType.SafeZone,
					       new Point(453, 12, 5), new Point(493, 42, 70)),
			new AreaBounds("Light Blue Wool Lobby", LocationType.SafeZone,
					       new Point(770, 76, -366), new Point(810, 106, -301)),
			new AreaBounds("Yellow Wool Lobby", LocationType.SafeZone,
					       new Point(1141, 39, 3), new Point(1181, 69, 68)),
			new AreaBounds("Bonus Wool Lobby", LocationType.SafeZone,
					       new Point(295, 10, -163), new Point(335, 40, -98)),

			new AreaBounds("Monument", LocationType.SafeZone,
					       new Point(1160, 0, -320), new Point(1400, 256, -115)),
			new AreaBounds("Mystic Grotto", LocationType.SafeZone,
					       new Point(317, 61, 309), new Point(383, 106, 392)),
			new AreaBounds("New Player Lobby (on Tutorial)", LocationType.SafeZone,
					       new Point(-1456, 0, -1216), new Point(-1425, 255, -1185)),

			// AdventureZone
			new AreaBounds("Commands", LocationType.AdventureZone,
					       new Point(-1584, 0, -1632), new Point(-1329, 255, -1377)),
			new AreaBounds("Siege Of Highwatch", LocationType.AdventureZone,
					       new Point(1505, 102, -178), new Point(1631, 256, -16)),
			new AreaBounds("Ctaz", LocationType.AdventureZone,
					       new Point(227, 10, 294), new Point(252, 256, 320)),
			new AreaBounds("Hermy", LocationType.AdventureZone,
					       new Point(-331, 86, 334), new Point(-310, 110, 355)),
	};

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

		for (AreaBounds area : mLocationBounds) {
			if (area.within(point)) {
				return area.getType();
			}
		}

		return LocationType.None;
	}
}
