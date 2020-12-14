package com.playmonumenta.plugins.utils;

import org.bukkit.util.Vector;

/**
 * A powerful vector util class that allows you to rotate the design<br>
 * of any particle design (lines, arcs, helixes, etc.) on the<br>
 * specified axis.<br>
 * Order of rotation (IMPORTANT): Z-axis, X-Axis, Y-Axis
 * @author NickNackGus
 *
 */

public class VectorUtils {

	// rotate vector "position" by angle "degrees" on the x-z-(2D)-plane (yaw; compass direction)
	public static Vector rotateYAxis(Vector position, double degrees) {
		double radians = Math.toRadians(degrees);
		double x = position.getX();
		double y = position.getY();
		double z = position.getZ();
		double cos = FastUtils.cos(radians);
		double sin = FastUtils.sin(radians);
		return (new Vector(x * cos - z * sin, y, z * cos + x * sin));
	}

	// rotate vector "position" by angle "degrees" on the y-z-(2D)-plane (pitch; looking up/down)
	public static Vector rotateXAxis(Vector position, double degrees) {
		// Angle is offset, since:
		// In Minecraft,     0 is straight ahead, -90 is straight up, and  90 is straight down
		// In this formula, 90 is straight ahead,   0 is straight up, and 180 is straight down
		double radians = Math.toRadians(degrees);
		double x = position.getX();
		double y = position.getY();
		double z = position.getZ();
		double cos = FastUtils.cos(radians);
		double sin = FastUtils.sin(radians);
		return (new Vector(x, y * cos + z * sin, z * cos - y * sin));
	}

	// rotate vector "position" by angle "degrees" on the x-y-(2D)-plane (roll; turn your screen, basically)
	public static Vector rotateZAxis(Vector position, double degrees) {
		double radians = Math.toRadians(degrees);
		double x = position.getX();
		double y = position.getY();
		double z = position.getZ();
		double cos = FastUtils.cos(radians);
		double sin = FastUtils.sin(radians);
		return (new Vector(x * cos + y * sin, y * cos - x * sin, z));
	}

	public static Vector crossProd(Vector u, Vector v) {
		Vector ret = new Vector();
		ret.setX((u.getY() * v.getZ()) - (u.getZ() * v.getY()));
		ret.setY((u.getZ() * v.getX()) - (u.getX() * v.getZ()));
		ret.setZ((u.getX() * v.getY()) - (u.getY() * v.getX()));
		return ret;
	}
}
