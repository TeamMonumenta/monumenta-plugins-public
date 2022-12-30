package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;


// TODO use PartialParticle
public class ParticleUtils {
	public enum BoundingBoxEdge {
		X_YMIN_ZMIN,
		X_YMIN_ZMAX,
		X_YMAX_ZMIN,
		X_YMAX_ZMAX,
		Y_XMIN_ZMIN,
		Y_XMIN_ZMAX,
		Y_XMAX_ZMIN,
		Y_XMAX_ZMAX,
		Z_XMIN_YMIN,
		Z_XMIN_YMAX,
		Z_XMAX_YMIN,
		Z_XMAX_YMAX
	}

	@FunctionalInterface
	public interface CleaveAnimation {

		void cleaveAnimation(Location loc, int rings);

	}

	// TODO use Consumer?
	@FunctionalInterface
	public interface SpawnParticleAction {
		/**
		 * Spawns a particle at the specified location
		 */
		void run(Location loc);
	}

	@FunctionalInterface
	public interface LineSlashAnimation {

		void lineSlashAnimation(Location loc, double middleProgress, double endProgress, boolean middle);
	}

	@FunctionalInterface
	public interface ParametricEquation {
		double equation(int t);
	}

	@FunctionalInterface
	public interface ParametricParticle {
		void run(Location loc, int t);
	}

	public static void explodingRingEffect(Plugin plugin, Location loc, double radius, double height, int ticks, Collection<Map.Entry<Double, SpawnParticleAction>> particles) {
		new BukkitRunnable() {
			double mCurrentRadius = 0;

			@Override
			public void run() {
				mCurrentRadius += radius / ticks;

				for (double theta = 0; theta <= 2 * Math.PI; theta = theta + Math.PI / (7 * radius)) {
					double x = mCurrentRadius * FastUtils.cos(theta);
					double y = (FastUtils.RANDOM.nextDouble() - 0.5) * height;
					double z = mCurrentRadius * FastUtils.sin(theta);
					loc.add(x, y, z);

					for (Map.Entry<Double, SpawnParticleAction> particle : particles) {
						if (FastUtils.RANDOM.nextDouble() < particle.getKey()) {
							particle.getValue().run(loc);
						}
					}

					loc.subtract(x, y, z);
				}
				if (mCurrentRadius >= radius) {
					this.cancel();
				}
			}

		}.runTaskTimer(plugin, 0, 1);
	}

	public static void explodingConeEffect(Plugin plugin, LivingEntity entity, float radius, Particle type1, double percent1, Particle type2, double percent2, double dotAngle) {
		explodingConeEffect(plugin, entity, entity.getEyeLocation().getDirection().setY(0).normalize(), radius, type1, percent1, type2, percent2, dotAngle);
	}

	public static void explodingConeEffect(Plugin plugin, LivingEntity entity, Vector dir, float radius, Particle type1, double percent1, Particle type2, double percent2, double dotAngle) {
		new BukkitRunnable() {
			double mCurrentRadius = Math.PI / 4;
			Location mLoc = entity.getLocation();
			World mWorld = mLoc.getWorld();
			Vector mDirection = dir.setY(0).normalize();

			@Override
			public void run() {
				mCurrentRadius = mCurrentRadius + 0.25 * Math.PI;
				for (double theta = 0; theta <= 2 * Math.PI; theta = theta + Math.PI / 64) {
					double x = mCurrentRadius * FastUtils.cos(theta);
					double y = 2 * Math.exp(-0.1 * mCurrentRadius) * FastUtils.sin(mCurrentRadius) + 0.5;
					double z = mCurrentRadius * FastUtils.sin(theta);
					mLoc.add(x, y, z);

					Vector toParticle = mLoc.toVector().subtract(entity.getLocation().toVector()).setY(0).normalize();

					if (mDirection.dot(toParticle) > dotAngle && FastUtils.RANDOM.nextDouble() < percent1) {
						mWorld.spawnParticle(type1, mLoc, 1);
					}

					mLoc.subtract(x, y, z);

					theta = theta + Math.PI / 64;

					x = mCurrentRadius * FastUtils.cos(theta);
					y = 2 * Math.exp(-0.1 * mCurrentRadius) * FastUtils.sin(mCurrentRadius) + 1.5;
					z = mCurrentRadius * FastUtils.sin(theta);
					mLoc.add(x, y, z);

					toParticle = mLoc.toVector().subtract(entity.getLocation().toVector()).setY(0).normalize();

					if (mDirection.dot(toParticle) > dotAngle && FastUtils.RANDOM.nextDouble() < percent2) {
						mWorld.spawnParticle(type2, mLoc, 1);
					}

					mLoc.subtract(x, y, z);
				}
				if (mCurrentRadius > radius) {
					this.cancel();
				}
			}

		}.runTaskTimer(plugin, 0, 1);
	}

	public static void tickBoundingBoxEdge(World world, BoundingBox bb, Color color, int count) {
		Particle.DustOptions dustOptions = new Particle.DustOptions(color, 0.2f);
		Vector bbSize = bb.getMax().clone().subtract(bb.getMin());
		NavigableMap<Double, BoundingBoxEdge> edgeWeights = new TreeMap<>();
		double largestKey = 0.0;
		for (BoundingBoxEdge edge : BoundingBoxEdge.values()) {
			double edgeSize;
			switch (edge) {
			case X_YMIN_ZMIN:
			case X_YMIN_ZMAX:
			case X_YMAX_ZMIN:
			case X_YMAX_ZMAX:
				edgeSize = bbSize.getX();
				break;
			case Y_XMIN_ZMIN:
			case Y_XMIN_ZMAX:
			case Y_XMAX_ZMIN:
			case Y_XMAX_ZMAX:
				edgeSize = bbSize.getY();
				break;
			default:
				edgeSize = bbSize.getZ();
			}
			// Ensure bounding boxes with a size of 0 still show up
			edgeSize += 0.001;
			largestKey += edgeSize;
			edgeWeights.put(largestKey, edge);
		}
		for (int i = 0; i < count; i++) {
			Map.Entry<Double, BoundingBoxEdge> edgeEntry = edgeWeights.higherEntry(largestKey * FastUtils.RANDOM.nextDouble());
			if (edgeEntry == null) {
				// The reviewdog says this is a thing? Why is this a thing?
				break;
			}
			BoundingBoxEdge edge = edgeEntry.getValue();

			double x;
			switch (edge) {
			case X_YMIN_ZMIN:
			case X_YMIN_ZMAX:
			case X_YMAX_ZMIN:
			case X_YMAX_ZMAX:
				x = bb.getMinX() + bbSize.getX() * FastUtils.RANDOM.nextDouble();
				break;
			case Y_XMIN_ZMIN:
			case Y_XMIN_ZMAX:
			case Z_XMIN_YMIN:
			case Z_XMIN_YMAX:
				x = bb.getMinX();
				break;
			default:
				x = bb.getMaxX();
			}

			double y;
			switch (edge) {
			case Y_XMIN_ZMIN:
			case Y_XMIN_ZMAX:
			case Y_XMAX_ZMIN:
			case Y_XMAX_ZMAX:
				y = bb.getMinY() + bbSize.getY() * FastUtils.RANDOM.nextDouble();
				break;
			case X_YMIN_ZMIN:
			case X_YMIN_ZMAX:
			case Z_XMIN_YMIN:
			case Z_XMAX_YMIN:
				y = bb.getMinY();
				break;
			default:
				y = bb.getMaxY();
			}

			double z;
			switch (edge) {
			case Z_XMIN_YMIN:
			case Z_XMIN_YMAX:
			case Z_XMAX_YMIN:
			case Z_XMAX_YMAX:
				z = bb.getMinZ() + bbSize.getZ() * FastUtils.RANDOM.nextDouble();
				break;
			case X_YMIN_ZMIN:
			case X_YMAX_ZMIN:
			case Y_XMIN_ZMIN:
			case Y_XMAX_ZMIN:
				z = bb.getMinZ();
				break;
			default:
				z = bb.getMaxZ();
			}

			world.spawnParticle(Particle.REDSTONE, x, y, z, 1, 0.0, 0.0, 0.0, dustOptions);
		}
	}

	public static void drawHalfArc(Location loc, double radius, double angle, double startingDegrees, double endingDegrees,
								   int rings, double spacing, CleaveAnimation cleaveAnim) {
		drawHalfArc(loc, radius, angle, startingDegrees, endingDegrees, rings, spacing, false, 40, cleaveAnim);
	}

	public static void drawHalfArc(Location loc, double radius, double angle, double startingDegrees, double endingDegrees,
								   int rings, double spacing, boolean reverse, int arcInc, CleaveAnimation cleaveAnim) {
		double radiusInc = (Math.PI / (endingDegrees - startingDegrees));

		loc = loc.clone();

		Location finalLoc = loc;
		new BukkitRunnable() {
			double mDegrees = startingDegrees;
			double mPI = 0;
			@Override
			public void run() {
				Vector vec;

				for (double d = mDegrees; d < mDegrees + arcInc; d += 5) {
					double radian1 = FastMath.toRadians(d);

					for (int i = 0; i < rings; i++) {
						double radiusSpacing = (reverse ? FastUtils.cos(mPI) : FastUtils.sin(mPI)) * (i * spacing);
						vec = new Vector(FastUtils.cos(radian1) * (radius + radiusSpacing),
							0, FastUtils.sin(radian1) * (radius + radiusSpacing));
						vec = VectorUtils.rotateZAxis(vec, angle);
						vec = VectorUtils.rotateXAxis(vec, finalLoc.getPitch());
						vec = VectorUtils.rotateYAxis(vec, finalLoc.getYaw());

						Location l = finalLoc.clone().add(vec);
						cleaveAnim.cleaveAnimation(l, i + 1);
					}

					mPI += radiusInc * 2.5;
					if (d >= endingDegrees) {
						this.cancel();
						return;
					}
				}

				mDegrees += 40;
			}

		}.runTaskTimer(com.playmonumenta.plugins.Plugin.getInstance(), 0, 1);
	}

	public static void drawParticleCircleExplosion(Player player, Location loc, double angle,
												   double radius, double yaw, double pitch, int points, float speed, boolean atOrigin, double radianAdd,
												   Particle... effects) {
		drawParticleCircleExplosion(player, loc, angle, radius, yaw, pitch, points, speed, atOrigin, radianAdd, 0, effects);
	}

	public static void drawParticleCircleExplosion(Player player, Location loc, double angle,
	                                               double radius, double yaw, double pitch, int points, float speed, boolean atOrigin, double radianAdd, double y,
	                                               Particle... effects) {
		drawParticleCircleExplosion(player, loc, angle, radius, yaw, pitch, points, speed, atOrigin, radianAdd, y, null, effects);
	}

	public static void drawParticleCircleExplosion(Player player, Location loc, double angle,
	                                               double radius, double yaw, double pitch, int points, float speed, boolean atOrigin, double radianAdd, double y,
	                                               @Nullable Object data, Particle... effects) {

		Vector vec;
		for (int i = 0; i < points; i++) {
			double radian = FastMath.toRadians(((360D / points) * i) + radianAdd);
			vec = new Vector(FastUtils.cos(radian) * radius, y, FastUtils.sin(radian) * radius);
			vec = VectorUtils.rotateZAxis(vec, angle);
			vec = VectorUtils.rotateXAxis(vec, loc.getPitch() + pitch);
			vec = VectorUtils.rotateYAxis(vec, loc.getYaw() + yaw);

			vec = vec.normalize();

			Location l = loc.clone();
			if (y > 0) {
				Vector nonYVec = new Vector(FastUtils.cos(radian) * radius, 0, FastUtils.sin(radian) * radius);
				nonYVec = VectorUtils.rotateZAxis(nonYVec, angle);
				nonYVec = VectorUtils.rotateXAxis(nonYVec, loc.getPitch() + pitch);
				nonYVec = VectorUtils.rotateYAxis(nonYVec, loc.getYaw() + yaw);
				l = l.add(nonYVec);
			}

			for (Particle effect : effects) {
				new PartialParticle(effect, atOrigin ? loc : l, 1, vec.getX(), vec.getY(), vec.getZ(), speed, data, true, 0)
					.minimumMultiplier(false).spawnAsPlayerActive(player);
			}
		}
	}

	public static void drawParticleLineSlash(Location loc, Vector dir, double angle, double length, double spacing, int duration, LineSlashAnimation animation) {
		Location l = loc.clone();
		l.setDirection(dir);

		List<Vector> points = new ArrayList<>();
		Vector vec = new Vector(0, 0, 1);
		vec = VectorUtils.rotateZAxis(vec, angle);
		vec = VectorUtils.rotateXAxis(vec, l.getPitch());
		vec = VectorUtils.rotateYAxis(vec, l.getYaw());
		vec = vec.normalize();

		for (double ln = -length; ln < length; ln += spacing) {
			Vector point = l.toVector().add(vec.clone().multiply(ln));
			points.add(point);
		}

		if (duration <= 0) {
			boolean midReached = false;
			for (int i = 0; i < points.size(); i++) {
				Vector point = points.get(i);
				boolean middle = !midReached && i == points.size() / 2;
				if (middle) {
					midReached = true;
				}
				animation.lineSlashAnimation(point.toLocation(loc.getWorld()),
					1D - (point.distance(l.toVector()) / length), (double) (i + 1) / points.size(), middle);
			}
		} else {
			new BukkitRunnable() {
				final int mPointsPerTick = (int) (points.size() * (1D / duration));
				int mT = 0;
				boolean mMidReached = false;
				@Override
				public void run() {


					for (int i = mPointsPerTick * mT; i < FastMath.min(points.size(), mPointsPerTick * (mT + 1)); i++) {
						Vector point = points.get(i);
						boolean middle = !mMidReached && i == points.size() / 2;
						if (middle) {
							mMidReached = true;
						}
						animation.lineSlashAnimation(point.toLocation(loc.getWorld()),
							1D - (point.distance(l.toVector()) / length), (double) (i + 1) / points.size(), middle);
					}
					mT++;

					if (mT >= duration) {
						this.cancel();
					}
				}

			}.runTaskTimer(com.playmonumenta.plugins.Plugin.getInstance(), 0, 1);
		}
	}

	public static void drawCleaveArc(Location loc,
									 double radius, double angle, double startingDegrees, double endingDegrees, int rings, double extraYaw,
									 double extraPitch, double spacing, double arcInc, CleaveAnimation cleaveAnim) {
		double radiusInc = (Math.PI / (endingDegrees - startingDegrees));
		double finalRadius = radius;

		loc = loc.clone();

		double finalAngle = angle;
		double finalExtraYaw = extraYaw;
		Location finalLoc = loc;
		new BukkitRunnable() {
			double mDegrees = startingDegrees;
			double mPI = 0;
			@Override
			public void run() {
				Vector vec;

				for (double d = mDegrees; d < mDegrees + arcInc; d += 5) {
					double radian1 = FastMath.toRadians(d);

					for (int i = 0; i < rings; i++) {
						double radiusSpacing = FastUtils.sin(mPI) * (i * spacing);
						vec = new Vector(FastUtils.cos(radian1) * (finalRadius + radiusSpacing),
							0, FastUtils.sin(radian1) * (finalRadius + radiusSpacing));
						vec = VectorUtils.rotateZAxis(vec, finalAngle);
						vec = VectorUtils.rotateXAxis(vec, finalLoc.getPitch() + extraPitch);
						vec = VectorUtils.rotateYAxis(vec, finalLoc.getYaw() + finalExtraYaw);

						Location l = finalLoc.clone().add(vec);
						cleaveAnim.cleaveAnimation(l, i + 1);
					}

					mPI += radiusInc * 5;
					if (d >= endingDegrees) {
						this.cancel();
						return;
					}
				}

				mDegrees += arcInc;
			}

		}.runTaskTimer(com.playmonumenta.plugins.Plugin.getInstance(), 0, 1);
	}

	public static Color getTransition(Color color, Color toColor, double percent) {
		int red = (int)Math.abs((percent * toColor.getRed()) + ((1 - percent) * color.getRed()));
		int green = (int)Math.abs((percent * toColor.getGreen()) + ((1 - percent) * color.getGreen()));
		int blue = (int)Math.abs((percent * toColor.getBlue()) + ((1 - percent) * color.getBlue()));

		return Color.fromRGB(Math.min(red, 255), Math.min(green, 255), Math.min(blue, 255));
	}

	public static Particle.DustOptions getTransition(Particle.DustOptions color, Particle.DustOptions toColor, double percent) {
		int red = (int)Math.abs((percent * toColor.getColor().getRed()) + ((1 - percent) * color.getColor().getRed()));
		int green = (int)Math.abs((percent * toColor.getColor().getGreen()) + ((1 - percent) * color.getColor().getGreen()));
		int blue = (int)Math.abs((percent * toColor.getColor().getBlue()) + ((1 - percent) * color.getColor().getBlue()));
		int size = (int)Math.abs((percent * toColor.getSize()) + ((1 - percent) * color.getSize()));

		return new Particle.DustOptions(Color.fromRGB(Math.min(red, 255), Math.min(green, 255), Math.min(blue, 255)), size);
	}


	/**
	 * Draw a particle curve in a vector space with parametric equations.
	 * @author Yelon Yagi a.k.a ProjektRed
	 * @param center Center (0, 0, 0) location of the vector space
	 * @param paraMin Minimum value of parameter
	 * @param paraMax Maximum value of parameter
	 * @param e1 Base vector 1
	 * @param e2 Base vector 2
	 * @param e3 Base vector 3
	 * @param eq1 Parameter equation 1
	 * @param eq2 Parameter equation 2
	 * @param eq3 Parameter equation 3
	 * @param pp What to do at every location generated by P.E.
	 */
	public static void drawCurve(Location center, int paraMin, int paraMax,
								 Vector e1, Vector e2, Vector e3,
								 ParametricEquation eq1, ParametricEquation eq2, ParametricEquation eq3,
								 ParametricParticle pp) {
		Location loc;
		for (int t = paraMin; t <= paraMax; t++) {
			loc = center.clone();
			loc.add(e1.clone().multiply(eq1.equation(t)));
			loc.add(e2.clone().multiply(eq2.equation(t)));
			loc.add(e3.clone().multiply(eq3.equation(t)));
			pp.run(loc, t);
		}
	}

	/**
	 * Draw a particle curve in a vector space with parametric equations,
	 * and the vector space is generated by front direction vector.
	 */
	public static void drawCurve(Location center, int paraMin, int paraMax, Vector front,
								 ParametricEquation f, ParametricEquation r, ParametricEquation u,
								 ParametricParticle pp) {
		Vector right = VectorUtils.rotateTargetDirection(front.clone(), 90, 0);
		Vector up = VectorUtils.rotateTargetDirection(front.clone(), 0, -90);
		drawCurve(center, paraMin, paraMax, front, right, up, f, r, u, pp);
	}

	/**
	 * Draw a particle curve in a vector space with parametric equations,
	 * and the vector space is generated by yaw and pitch.
	 */
	public static void drawCurve(Location center, int paraMin, int paraMax, float yaw, float pitch,
								 ParametricEquation f, ParametricEquation r, ParametricEquation u,
								 ParametricParticle pp) {
		Vector front = VectorUtils.rotationToVector(yaw, pitch);
		Vector right = VectorUtils.rotateTargetDirection(front.clone(), 90, 0);
		Vector up = VectorUtils.rotateTargetDirection(front.clone(), 0, -90);
		drawCurve(center, paraMin, paraMax, front, right, up, f, r, u, pp);
	}

	/**
	 * Draw a ring with particles
	 * @param center Center location of the ring
	 * @param units How many units consisting the ring
	 * @param normal Normal vector of the plane where the ring is
	 * @param radius Radius of the ring
	 * @param pp Particle action at each unit
	 */
	public static void drawRing(Location center, int units, Vector normal, double radius, ParametricParticle pp) {
		ParticleUtils.drawCurve(center, 1, units, normal.clone().normalize(),
			t -> 0,
			t -> FastUtils.cos(2 * 3.1416 * t / units) * radius,
			t -> FastUtils.sin(2 * 3.1416 * t / units) * radius,
			pp
		);
	}
}
