package com.playmonumenta.plugins.utils;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

/**
 * A powerful vector util class that allows you to rotate the design<br>
 * of any particle design (lines, arcs, helixes, etc.) on the<br>
 * specified axis.<br>
 * Order of rotation (IMPORTANT): Z-axis, X-Axis, Y-Axis
 *
 * (Now also does other things.)
 *
 * @author NickNackGus
 */

public class VectorUtils {

	// rotate vector "position" by angle "degrees" on the x-z-(2D)-plane (yaw; compass direction)
	public static Vector rotateYAxis(Vector position, double degrees) {
		double x = position.getX();
		double y = position.getY();
		double z = position.getZ();
		double cos = FastUtils.cosDeg(degrees);
		double sin = FastUtils.sinDeg(degrees);
		return new Vector(x * cos - z * sin, y, z * cos + x * sin);
	}

	// rotate vector "position" by angle "degrees" on the y-z-(2D)-plane (pitch; looking up/down)
	public static Vector rotateXAxis(Vector position, double degrees) {
		// Angle is negative, since:
		// This is 1:1 with Minecraft; -90 is up, 90 is down, 0 is straight ahead.
		double x = position.getX();
		double y = position.getY();
		double z = position.getZ();
		double cos = FastUtils.cosDeg(degrees);
		double sin = FastUtils.sinDeg(degrees);
		return new Vector(x, y * cos - z * sin, z * cos + y * sin);
	}

	// rotate vector "position" by angle "degrees" on the x-y-(2D)-plane (roll; turn your screen, basically)
	public static Vector rotateZAxis(Vector position, double degrees) {
		double x = position.getX();
		double y = position.getY();
		double z = position.getZ();
		double cos = FastUtils.cosDeg(degrees);
		double sin = FastUtils.sinDeg(degrees);
		return new Vector(x * cos + y * sin, y * cos - x * sin, z);
	}

	public static Vector crossProd(Vector u, Vector v) {
		Vector ret = new Vector();
		ret.setX((u.getY() * v.getZ()) - (u.getZ() * v.getY()));
		ret.setY((u.getZ() * v.getX()) - (u.getX() * v.getZ()));
		ret.setZ((u.getX() * v.getY()) - (u.getY() * v.getX()));
		return ret;
	}

	public static boolean isAngleWithin(
		Vector v1,
		Vector v2,
		double degrees
	) {
		return v1.angle(v2) < Math.toRadians(degrees);
	}

	// convert direction vector to [yaw, pitch] in degrees
	public static double[] vectorToRotation(Vector dir) {
		double[] ret = new double[2];
		Vector nor = dir.clone().normalize();
		//For Minecraft coords are different with math coords, we need to flip X and Y.
		ret[1] = Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, -nor.getY()))));
		ret[0] = Math.toDegrees(Math.atan2(-nor.getX(), nor.getZ()));
		return ret;
	}

	// convert [yaw, pitch] in degrees to direction vector
	public static Vector rotationToVector(double yaw, double pitch) {
		double cosPitch = FastUtils.cosDeg(pitch);
		double x = -FastUtils.sinDeg(yaw) * cosPitch;
		double z = FastUtils.cosDeg(yaw) * cosPitch;
		double y = -FastUtils.sinDeg(pitch);
		return new Vector(x, y, z);
	}

	// similar to volley generator?
	public static Vector rotateTargetDirection(Vector targetDir, double yaw, double pitch) {
		if (yaw == 0 && pitch == 0) {
			return targetDir;
		} else {
			double[] targetAngle = VectorUtils.vectorToRotation(targetDir);
			// Start with the assumption the target is at due South (yaw 0.0, pitch 0.0, no offset)
			Vector dir = new Vector(0.0, 0.0, targetDir.length());
			// Apply pitch/yaw offset to get start pattern
			dir = VectorUtils.rotateXAxis(dir, pitch);
			dir = VectorUtils.rotateYAxis(dir, yaw);
			// Apply target pitch/yaw to rotate that pattern to match the direction
			dir = VectorUtils.rotateXAxis(dir, targetAngle[1]);
			dir = VectorUtils.rotateYAxis(dir, targetAngle[0]);
			return dir;
		}
	}

	/**
	 * Calculates the intersection point of a line and a plane.
	 * <p>
	 * Returns null if the line and plane are parallel (i.e. they either don't intersect or the line is part of the plane).
	 *
	 * @param linePoint     A point on the line
	 * @param lineDirection Direction of the line
	 * @param planePoint    A point on the plane
	 * @param planeNormal   Normal of the plane
	 */
	public static @Nullable Vector linePlaneIntersection(Vector linePoint, Vector lineDirection, Vector planePoint, Vector planeNormal) {
		double cosAngle = lineDirection.dot(planeNormal);
		if (cosAngle == 0) {
			return null;
		}
		double intersectionDistance = planePoint.clone().subtract(linePoint).dot(planeNormal) / cosAngle;
		return linePoint.clone().add(lineDirection.clone().multiply(intersectionDistance));
	}

	/**
	 * Calculates the intersection point of a ray and a plane.
	 * <p>
	 * Returns null if the ray and plane don't intersect (i.e. if the ray is parallel to the plane or pointing away from it).
	 *
	 * @param rayStart     Start of the ray
	 * @param rayDirection Direction of the ray
	 * @param planePoint   A point on the plane
	 * @param planeNormal  Normal of the plane
	 */
	public static @Nullable Vector rayPlaneIntersection(Vector rayStart, Vector rayDirection, Vector planePoint, Vector planeNormal) {
		double cosAngle = rayDirection.dot(planeNormal);
		if (cosAngle == 0) {
			return null;
		}
		double intersectionDistance = planePoint.clone().subtract(rayStart).dot(planeNormal) / cosAngle;
		if (intersectionDistance < 0) {
			return null;
		}
		return rayStart.clone().add(rayDirection.clone().multiply(intersectionDistance));
	}

	public static Vector randomUnitVector() {
		Vector v = new Vector(FastUtils.RANDOM.nextGaussian(), FastUtils.RANDOM.nextGaussian(), FastUtils.RANDOM.nextGaussian());
		v.normalize();
		if (!Double.isFinite(v.getX())) { // we got the 0-vector (or one close enough to 0)
			return new Vector(1, 0, 0);
		}
		return v;
	}

	/**
	 * Creates a list of direction vectors that are not close to each other.
	 * Will try up to 100 times per vector to make a random vector that isn't close to another vector already.
	 * If this limit is exceeded, completely random vectors are used instead for any remaining vectors (they will still not point in the disallowed direction though).
	 *
	 * @param count               Number of vectors to make
	 * @param length              Length of each vector
	 * @param maxCloseness        Minimum angle between vectors, in degrees
	 * @param disallowedDirection An optional direction where none of the created vectors may point to. Must be a unit vector.
	 * @param disallowedCloseness How close to the disallowed direction vectors may not be, in degrees. Must not be greater than 90.
	 */
	public static List<Vector> semiRandomDirections(int count, double length, double maxCloseness, @Nullable Vector disallowedDirection, double disallowedCloseness) {
		double maxClosenessCos = FastUtils.cosDeg(maxCloseness);
		double disallowedClosenessCos = FastUtils.cosDeg(disallowedCloseness);
		List<Vector> result = new ArrayList<>(count);
		int triesRemaining = 100 * count;
		for (int i = 0; i < count; i++) {
			Vector dir = randomUnitVector();
			if (disallowedDirection != null && dir.dot(disallowedDirection) > disallowedClosenessCos) {
				dir.multiply(-1);
			}
			if (triesRemaining > 0 && result.stream().anyMatch(v -> v.dot(dir) > maxClosenessCos)) {
				i--;
				triesRemaining--;
				continue;
			}
			result.add(dir);
		}
		result.forEach(v -> v.multiply(length));
		return result;
	}

}
