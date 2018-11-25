package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.point.AreaBounds;
import com.playmonumenta.plugins.point.Point;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;

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

	public static Location getEntityCenter(Entity e) {
		return e.getLocation().add(0, e.getHeight() / 2, 0);
	}

	public static boolean isLosBlockingBlock(Material mat) {
		return mat.isOccluding();
	}

	public static boolean isPathBlockingBlock(Material mat) {
		return mat.isSolid() || mat.equals(Material.LAVA);
	}

	public static boolean isValidBoatLocation(Location loc) {
		Block block = loc.getBlock();
		if (block.isLiquid() || loc.subtract(0, 1, 0).getBlock().isLiquid()) {
			return true;
		}

		/*
		 * Check up to 50 blocks underneath the location. Stop when
		 * a non-air block is hit. If it's a liquid, this is allowed, otherwise it's not
		 */
		loc = loc.clone();
		for (int i = loc.getBlockY(); i > (Math.max(0, loc.getBlockY() - 50)); i--) {
			loc.setY(i);
			block = loc.getBlock();
			if (block.isLiquid()) {
				return true;
			} else if (!block.isEmpty()) {
				return false;
			}
		}

		return false;
	}
}
