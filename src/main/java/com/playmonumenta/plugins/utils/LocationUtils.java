package com.playmonumenta.plugins.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.point.AreaBounds;
import com.playmonumenta.plugins.point.Point;

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

	public static final AreaBounds OLDLABS = new AreaBounds("oldLabs", LocationType.None, new Point(-416.0, 48.0, -976.0), new Point(574.0, 200.0, -750.0));

	public static LocationType getLocationType(Plugin plugin, Entity entity) {
		return getLocationType(plugin, entity.getLocation());
	}

	public static LocationType getLocationType(Plugin plugin, Location location) {
		return getLocationType(plugin, new Point(location));
	}

	public static LocationType getLocationType(Plugin plugin, Point point) {
		if (plugin.mServerProperties.getIsTownWorld()) {
			return LocationType.Capital;
		}


		if (OLDLABS.within(point)) {
			return OLDLABS.getType();
		}

		for (AreaBounds area : plugin.mServerProperties.mLocationBounds) {
			if (area.within(point)) {
				return area.getType();
			}
		}

		return LocationType.None;
	}

	public static Vector getDirectionTo(Location to, Location from) {
		Vector vFrom = from.toVector();
		Vector vTo = to.toVector();
		return vTo.subtract(vFrom).normalize();
	}

	public static boolean isLosBlockingBlock(Material mat) {
		if (mat.equals(Material.AIR) ||
		    mat.equals(Material.GLASS) ||
		    mat.equals(Material.STAINED_GLASS) ||
		    mat.equals(Material.VINE) ||
		    mat.equals(Material.DOUBLE_PLANT) ||
		    mat.equals(Material.LONG_GRASS) ||
		    mat.equals(Material.YELLOW_FLOWER) ||
		    mat.equals(Material.RED_ROSE) ||
		    mat.equals(Material.WEB) ||
		    mat.equals(Material.WATER) ||
		    mat.equals(Material.STATIONARY_WATER) ||
		    mat.equals(Material.LAVA) ||
		    mat.equals(Material.STATIONARY_LAVA) ||
		    mat.equals(Material.CARPET) ||
		    ItemUtils.isDoor(mat)) {
			return false;
		}

		return true;
	}

	public static boolean isPathBlockingBlock(Material mat) {
		if (mat.equals(Material.AIR) ||
		    mat.equals(Material.VINE) ||
		    mat.equals(Material.DOUBLE_PLANT) ||
		    mat.equals(Material.LONG_GRASS) ||
		    mat.equals(Material.YELLOW_FLOWER) ||
		    mat.equals(Material.RED_ROSE) ||
		    mat.equals(Material.WEB) ||
		    mat.equals(Material.WATER) ||
		    mat.equals(Material.STATIONARY_WATER) ||
		    mat.equals(Material.CARPET) ||
		    ItemUtils.isDoor(mat)) {
			return false;
		}

		return true;
	}
}
