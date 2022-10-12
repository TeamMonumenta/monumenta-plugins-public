package com.playmonumenta.plugins.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.math3.util.MathUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A hitbox represents a shape in 3D-space that can be used to test for intersection (i.e. hits) of {@link BoundingBox}es, most notable those of mobs and players.
 * There's various subclasses for specific shapes of hitbox, such as {@link SphereHitbox}, and a generic class {@link ApproximateFreeformHitbox} to create hitboxes from just a simple test function.
 * {@link Hitbox} has a few utility methods to create some such approximate shapes like {@link #approximateCone(Location, double, double) cones}.
 */
public abstract class Hitbox {

	/**
	 * A simple AABB (axis-aligned bounding box) hitbox. Basically turns a {@link BoundingBox} into a {@link Hitbox}.
	 */
	public static class AABBHitbox extends Hitbox {

		private final World mWorld;
		private final BoundingBox mAabb;

		public AABBHitbox(World world, BoundingBox aabb) {
			this.mWorld = world;
			this.mAabb = aabb.clone();
		}

		@Override
		public boolean contains(Vector vector) {
			return mAabb.contains(vector);
		}

		@Override
		public boolean intersects(BoundingBox bbox) {
			return bbox.overlaps(mAabb);
		}

		@Override
		public BoundingBox getBoundingBox() {
			return mAabb.clone();
		}

		@Override
		public World getWorld() {
			return mWorld;
		}
	}

	/**
	 * A hitbox representing an upright cylinder (aligned along the y-axis).
	 */
	public static class UprightCylinderHitbox extends Hitbox {

		private final Location mBaseCenter;
		private final double mHeight;
		private final double mRadius;
		private final double mRadiusSquared;

		public UprightCylinderHitbox(Location baseCenter, double height, double radius) {
			this.mBaseCenter = baseCenter;
			this.mHeight = height;
			this.mRadius = radius;
			mRadiusSquared = radius * radius;
		}

		@Override
		public boolean contains(Vector vector) {
			return vector.getY() >= mBaseCenter.getY() && vector.getY() <= mBaseCenter.getY() + mHeight
				       && vector.clone().setY(0).distanceSquared(mBaseCenter.toVector().setY(0)) <= mRadiusSquared;
		}

		@Override
		public boolean intersects(BoundingBox bbox) {
			BoundingBox thisBB = getBoundingBox();
			if (!thisBB.overlaps(bbox)) {
				return false;
			}

			// When the bounding boxes overlap, the only thing left to check is if the XZ-rectangle of the BB intersects the base circle
			// this is either the case when a corner of the rectangle is in the circle, or when two corners are on different "sides" of the circle
			// (or conversely, the rectangle doesn't intersect IFF all its corners are outside the circle and all in the same "quadrant")
			double minX = bbox.getMinX() - mBaseCenter.getX();
			double maxX = bbox.getMaxX() - mBaseCenter.getX();
			double minZ = bbox.getMinZ() - mBaseCenter.getZ();
			double maxZ = bbox.getMaxZ() - mBaseCenter.getZ();
			return minX * maxX < 0
				       || minZ * maxZ < 0
				       || minX * minX + minZ * minZ <= mRadiusSquared
				       || minX * minX + maxZ * maxZ <= mRadiusSquared
				       || maxX * maxX + minZ * minZ <= mRadiusSquared
				       || maxX * maxX + maxZ * maxZ <= mRadiusSquared;
		}

		@Override
		public BoundingBox getBoundingBox() {
			return new BoundingBox(mBaseCenter.getX() - mRadius, mBaseCenter.getY(), mBaseCenter.getZ() - mRadius,
				mBaseCenter.getX() + mRadius, mBaseCenter.getY() + mHeight, mBaseCenter.getZ() + mRadius);
		}

		@Override
		public World getWorld() {
			return mBaseCenter.getWorld();
		}
	}

	public static class SphereHitbox extends Hitbox {

		private final Location mCenter;
		private final double mRadius;
		private final double mRadiusSquared;

		public SphereHitbox(Location center, double radius) {
			this.mCenter = center;
			this.mRadius = radius;
			mRadiusSquared = radius * radius;
		}

		@Override
		public boolean contains(Vector vector) {
			return vector.distanceSquared(mCenter.toVector()) <= mRadiusSquared;
		}

		@Override
		public boolean intersects(BoundingBox bbox) {
			double distX = Math.max(bbox.getMinX() - mCenter.getX(), mCenter.getX() - bbox.getMaxX());
			double distY = Math.max(bbox.getMinY() - mCenter.getY(), mCenter.getY() - bbox.getMaxY());
			double distZ = Math.max(bbox.getMinZ() - mCenter.getZ(), mCenter.getZ() - bbox.getMaxZ());
			double distSquared = (distX > 0 ? distX * distX : 0) + (distY > 0 ? distY * distY : 0) + (distZ > 0 ? distZ * distZ : 0);
			return distSquared <= mRadiusSquared;
		}

		@Override
		public BoundingBox getBoundingBox() {
			return new BoundingBox(mCenter.getX() - mRadius, mCenter.getY() - mRadius, mCenter.getZ() - mRadius,
				mCenter.getX() + mRadius, mCenter.getY() + mRadius, mCenter.getZ() + mRadius);
		}

		@Override
		public World getWorld() {
			return mCenter.getWorld();
		}
	}

	/**
	 * Free-form hitbox that checks intersection using multiple points on mobs' bounding boxes
	 * (at least all 8 corners, and potentially more inside depending on the set accuracy)
	 */
	public static class ApproximateFreeformHitbox extends Hitbox {

		private final World mWorld;
		private final BoundingBox mBoundingBox;
		private final Predicate<Vector> mContains;
		private double mAccuracy;

		public ApproximateFreeformHitbox(World world, BoundingBox boundingBox, Predicate<Vector> contains) {
			this(world, boundingBox, contains, 1);
		}

		public ApproximateFreeformHitbox(World world, BoundingBox boundingBox, Predicate<Vector> contains, double accuracy) {
			this.mWorld = world;
			this.mBoundingBox = boundingBox;
			this.mContains = contains;
			this.mAccuracy = accuracy;
		}

		@Override
		public boolean contains(Vector vector) {
			return mContains.test(vector);
		}

		@Override
		public boolean intersects(BoundingBox bbox) {
			if (!mBoundingBox.overlaps(bbox)) {
				return false;
			}
			double sizeX = bbox.getWidthX();
			int stepsX = 1 + (int) Math.ceil(sizeX / mAccuracy);
			double stepX = stepsX == 1 ? 0 : sizeX / (stepsX - 1);
			double sizeY = bbox.getHeight();
			int stepsY = 1 + (int) Math.ceil(sizeY / mAccuracy);
			double stepY = stepsY == 1 ? 0 : sizeY / (stepsY - 1);
			double sizeZ = bbox.getWidthX();
			int stepsZ = 1 + (int) Math.ceil(sizeZ / mAccuracy);
			double stepZ = stepsZ == 1 ? 0 : sizeZ / (stepsZ - 1);
			Vector test = new Vector();
			for (int x = 0; x < stepsX; x++) {
				test.setX(bbox.getMinX() + stepX * x);
				for (int y = 0; y < stepsY; y++) {
					test.setY(bbox.getMinY() + stepY * y);
					for (int z = 0; z < stepsZ; z++) {
						test.setZ(bbox.getMinZ() + stepZ * z);
						if (mContains.test(test)) {
							return true;
						}
					}
				}
			}
			return false;
		}

		public ApproximateFreeformHitbox accuracy(double accuracy) {
			mAccuracy = accuracy;
			return this;
		}

		@Override
		public BoundingBox getBoundingBox() {
			return mBoundingBox.clone();
		}

		@Override
		public World getWorld() {
			return mWorld;
		}
	}

	/**
	 * Creates an approximate cone-shaped hitbox, using a spherical base.
	 *
	 * @param start        Start (tip) of the cone, including direction
	 * @param halfAngleRad Cone half-angle in radians (half of the full angle)
	 * @param radius       Length of the cone
	 */
	public static ApproximateFreeformHitbox approximateCone(Location start, double radius, double halfAngleRad) {
		double radiusSquared = radius * radius;
		double cosAngle = halfAngleRad >= Math.PI ? -1 : Math.cos(halfAngleRad);
		Vector direction = start.getDirection();
		Vector startVector = start.toVector();
		return new ApproximateFreeformHitbox(start.getWorld(),
			new BoundingBox(start.getX() - radius, start.getY() - radius, start.getZ() - radius,
				start.getX() + radius, start.getY() + radius, start.getZ() + radius),
			test -> test.distanceSquared(startVector) <= radiusSquared
				        && test.clone().subtract(startVector).normalize().dot(direction) >= cosAngle
		);
	}

	/**
	 * Creates an approximate hitbox representing a segment of an upright cylinder (aligned along the y-axis).
	 *
	 * @param baseCenter   Center of the cylinder's base, including direction of the segment
	 * @param height       Height of the cylinder
	 * @param radius       Radius of the cylinder
	 * @param halfAngleRad Segment half-angle in radians (half of the full angle)
	 */
	public static ApproximateFreeformHitbox approximateCylinderSegment(Location baseCenter, double height, double radius, double halfAngleRad) {
		double radiusSquared = radius * radius;
		double baseYaw = Math.toRadians(baseCenter.getYaw() + 90); // +90 as Bukkit yaw starts at the Z axis, not X
		Vector baseCenterVector = baseCenter.toVector();
		return new ApproximateFreeformHitbox(baseCenter.getWorld(),
			new BoundingBox(baseCenter.getX() - radius, baseCenter.getY(), baseCenter.getZ() - radius,
				baseCenter.getX() + radius, baseCenter.getY() + height, baseCenter.getZ() + radius),
			test -> test.getY() >= baseCenterVector.getY()
				        && test.getY() <= baseCenterVector.getY() + height
				        && test.clone().setY(baseCenterVector.getY()).distanceSquared(baseCenterVector) <= radiusSquared
				        && Math.abs(MathUtils.normalizeAngle(Math.atan2(test.getZ() - baseCenterVector.getZ(), test.getX() - baseCenterVector.getX()) - baseYaw, 0)) <= halfAngleRad
		);
	}

	/**
	 * Creates a new hitbox as the union of this hitbox and the given hitbox.
	 * Entities will be in this union hitbox if they are in either (or both) of the partial hitboxes.
	 * <p>
	 * The returned hitbox is accurate if both passed hitboxes are accurate - otherwise, the inaccuracies are inherited for their parts of the union.
	 * <p>
	 * NB: When combining approximate hitboxes, the accuracies of the given hitboxes <i>are</i> used (in contrast to {@link #approximateIntersection(Hitbox) approximateIntersection}).
	 */
	public Hitbox union(Hitbox other) {
		Hitbox self = this;
		return new Hitbox() {
			@Override
			public boolean contains(Vector vector) {
				return self.contains(vector) || other.contains(vector);
			}

			@Override
			public boolean intersects(BoundingBox bbox) {
				return self.intersects(bbox) || other.intersects(bbox);
			}

			@Override
			public BoundingBox getBoundingBox() {
				return self.getBoundingBox().union(other.getBoundingBox());
			}

			@Override
			public World getWorld() {
				return self.getWorld();
			}
		};
	}

	/**
	 * Creates a new hitbox as the intersection of this hitbox and the given hitbox.
	 * Entities will be in this intersection hitbox if they have at least one (tested) point that is contained in both partial hitboxes.
	 * <p>
	 * NB: When combining approximate hitboxes, the accuracies of the given hitboxes are not used - only their {@code contains} tests are used (plus their bounding boxes).
	 * Set the accuracy on the returned intersection instead.
	 */
	public ApproximateFreeformHitbox approximateIntersection(Hitbox other) {
		return new ApproximateFreeformHitbox(getWorld(),
			getBoundingBox().intersection(other.getBoundingBox()),
			test -> contains(test) && other.contains(test));
	}

	public abstract boolean contains(Vector vector);

	public abstract boolean intersects(BoundingBox bbox);

	/**
	 * Returns a bounding box that completely contains this hitbox. Used to grab an initial list of mobs to then do more accurate intersection tests with.
	 */
	public abstract BoundingBox getBoundingBox();

	public abstract World getWorld();

	/**
	 * Gets a modifiable list of hostile mobs that are hit by this hitbox.
	 */
	public List<LivingEntity> getHitMobs() {
		return getHitMobs(null);
	}

	/**
	 * Gets a modifiable list of hostile mobs that are hit by this hitbox. The provided mob is excluded from the list.
	 */
	public List<LivingEntity> getHitMobs(@Nullable LivingEntity exclude) {
		BoundingBox boundingBox = getBoundingBox();
		return EntityUtils.getNearbyMobs(boundingBox.getCenter().toLocation(getWorld()), boundingBox.getWidthX() / 2, boundingBox.getHeight() / 2, boundingBox.getWidthZ() / 2,
			entity -> entity != exclude && EntityUtils.isHostileMob(entity) && intersects(entity.getBoundingBox()));
	}

	/**
	 * Gets a modifiable list of players that are hit by this hitbox.
	 */
	public List<Player> getHitPlayers(boolean includeNonTargetable) {
		return getHitPlayers(null, includeNonTargetable);
	}

	/**
	 * Gets a modifiable list of players that are hit by this hitbox. The provided player is excluded from the list.
	 */
	public List<Player> getHitPlayers(@Nullable Player exclude, boolean includeNonTargetable) {
		return getWorld().getPlayers().stream()
			       .filter(player -> player.getGameMode() != GameMode.SPECTATOR
				                         && player != exclude
				                         && player.getHealth() > 0
				                         && player.isValid()
				                         && (includeNonTargetable || !AbilityUtils.isStealthed(player))
				                         && intersects(player.getBoundingBox()))
			       .collect(Collectors.toCollection(ArrayList::new));
	}

}
