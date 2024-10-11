package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.particle.ParticleCategory;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
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
	public interface PreciseParametricEquation {
		double equation(double t);
	}

	@FunctionalInterface
	public interface ParametricParticle {
		void run(Location loc, int t);
	}

	@FunctionalInterface
	public interface PreciseParametricParticle {
		void run(Location loc, double t);
	}

	public static void explodingRingEffect(Plugin plugin, Location loc, double radius, double height, int ticks, double chance, SpawnParticleAction spawnParticleAction) {
		explodingRingEffect(plugin, loc, radius, height, ticks, List.of(new AbstractMap.SimpleEntry<>(chance, spawnParticleAction)));
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

	public static void explodingConeEffectSkill(Plugin plugin, LivingEntity entity, float radius, Particle type1, double percent1, Particle type2, double percent2, double dotAngle, Player player) {
		explodingConeEffect(plugin, entity, entity.getEyeLocation().getDirection().setY(0).normalize(), radius,
			(loc) -> {
				if (FastUtils.randomDoubleInRange(0, 1) < percent1) {
					new PartialParticle(type1, loc).spawnAsPlayerActive(player);
				}
			},
			(loc) -> {
				if (FastUtils.randomDoubleInRange(0, 1) < percent2) {
					new PartialParticle(type2, loc).spawnAsPlayerActive(player);
				}
			}, dotAngle);
	}

	public static void explodingConeEffect(Plugin plugin, LivingEntity entity, Vector dir, float radius, SpawnParticleAction spawnParticleAction1, SpawnParticleAction spawnParticleAction2, double dotAngle) {
		new BukkitRunnable() {
			double mCurrentRadius = Math.PI / 4;
			final Location mLoc = entity.getLocation();
			final Vector mDirection = dir.setY(0).normalize();

			@Override
			public void run() {
				mCurrentRadius = mCurrentRadius + 0.25 * Math.PI;
				for (double theta = 0; theta <= 2 * Math.PI; theta = theta + Math.PI / 64) {
					double x = mCurrentRadius * FastUtils.cos(theta);
					double y = 2 * Math.exp(-0.1 * mCurrentRadius) * FastUtils.sin(mCurrentRadius) + 0.5;
					double z = mCurrentRadius * FastUtils.sin(theta);
					mLoc.add(x, y, z);

					Vector toParticle = mLoc.toVector().subtract(entity.getLocation().toVector()).setY(0).normalize();

					if (mDirection.dot(toParticle) > dotAngle) {
						spawnParticleAction1.run(mLoc);
					}

					mLoc.subtract(x, y, z);

					theta = theta + Math.PI / 64;

					x = mCurrentRadius * FastUtils.cos(theta);
					y = 2 * Math.exp(-0.1 * mCurrentRadius) * FastUtils.sin(mCurrentRadius) + 1.5;
					z = mCurrentRadius * FastUtils.sin(theta);
					mLoc.add(x, y, z);

					toParticle = mLoc.toVector().subtract(entity.getLocation().toVector()).setY(0).normalize();

					if (mDirection.dot(toParticle) > dotAngle) {
						spawnParticleAction2.run(mLoc);
					}

					mLoc.subtract(x, y, z);
				}
				if (mCurrentRadius > radius) {
					this.cancel();
				}
			}

		}.runTaskTimer(plugin, 0, 1);
	}

	public static void tickBoundingBoxEdge(LivingEntity entity, World world, BoundingBox bb, Color color, int count) {
		Particle.DustOptions dustOptions = new Particle.DustOptions(color, 0.2f);
		Vector bbSize = bb.getMax().clone().subtract(bb.getMin());
		NavigableMap<Double, BoundingBoxEdge> edgeWeights = new TreeMap<>();
		double largestKey = 0.0;
		for (BoundingBoxEdge edge : BoundingBoxEdge.values()) {
			double edgeSize = switch (edge) {
				case X_YMIN_ZMIN, X_YMIN_ZMAX, X_YMAX_ZMIN, X_YMAX_ZMAX -> bbSize.getX();
				case Y_XMIN_ZMIN, Y_XMIN_ZMAX, Y_XMAX_ZMIN, Y_XMAX_ZMAX -> bbSize.getY();
				default -> bbSize.getZ();
			};
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

			double x = switch (edge) {
				case X_YMIN_ZMIN, X_YMIN_ZMAX, X_YMAX_ZMIN, X_YMAX_ZMAX ->
					bb.getMinX() + bbSize.getX() * FastUtils.RANDOM.nextDouble();
				case Y_XMIN_ZMIN, Y_XMIN_ZMAX, Z_XMIN_YMIN, Z_XMIN_YMAX ->
					bb.getMinX();
				default -> bb.getMaxX();
			};

			double y = switch (edge) {
				case Y_XMIN_ZMIN, Y_XMIN_ZMAX, Y_XMAX_ZMIN, Y_XMAX_ZMAX ->
					bb.getMinY() + bbSize.getY() * FastUtils.RANDOM.nextDouble();
				case X_YMIN_ZMIN, X_YMIN_ZMAX, Z_XMIN_YMIN, Z_XMAX_YMIN ->
					bb.getMinY();
				default -> bb.getMaxY();
			};

			double z = switch (edge) {
				case Z_XMIN_YMIN, Z_XMIN_YMAX, Z_XMAX_YMIN, Z_XMAX_YMAX ->
					bb.getMinZ() + bbSize.getZ() * FastUtils.RANDOM.nextDouble();
				case X_YMIN_ZMIN, X_YMAX_ZMIN, Y_XMIN_ZMIN, Y_XMAX_ZMIN ->
					bb.getMinZ();
				default -> bb.getMaxZ();
			};

			new PartialParticle(Particle.REDSTONE, new Location(world, x, y, z), 1, 0.0, 0.0, 0.0, dustOptions).spawnAsEntityActive(entity);
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

				mDegrees += arcInc;
			}

		}.runTaskTimer(com.playmonumenta.plugins.Plugin.getInstance(), 0, 1);
	}

	public static void drawParticleCircleExplosion(LivingEntity entity, Location loc, double angle,
												   double radius, double yaw, double pitch, int points, float speed, boolean atOrigin, double radianAdd,
												   Particle... effects) {
		drawParticleCircleExplosion(entity, loc, angle, radius, yaw, pitch, points, speed, atOrigin, radianAdd, 0, effects);
	}

	public static void drawParticleCircleExplosion(LivingEntity entity, Location loc, double angle,
	                                               double radius, double yaw, double pitch, int points, float speed, boolean atOrigin, double radianAdd, double y,
	                                               Particle... effects) {
		drawParticleCircleExplosion(entity, loc, angle, radius, yaw, pitch, points, speed, atOrigin, radianAdd, y, null, effects);
	}

	public static void drawParticleCircleExplosion(LivingEntity entity, Location loc, double angle,
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
		if (entity instanceof Player player) {
			for (Particle effect : effects) {
				new PartialParticle(effect, atOrigin ? loc : l, 1, vec.getX(), vec.getY(), vec.getZ(), speed, data, true, 0)
					.spawnAsPlayerActive(player);
			}
		} else {
				for (Particle effect : effects) {
					new PartialParticle(effect, atOrigin ? loc : l, 1, vec.getX(), vec.getY(), vec.getZ(), speed, data, true, 0)
						.spawnAsEntityActive(entity);
				}
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
									 final double radius, final double angle, double startingDegrees, double endingDegrees,
									 final int rings, final double extraYaw,
									 double extraPitch, double spacing, double arcInc, CleaveAnimation cleaveAnim) {
		double radiusInc = (Math.PI / (endingDegrees - startingDegrees));

		Location finalLoc = loc.clone();
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
						vec = new Vector(FastUtils.cos(radian1) * (radius + radiusSpacing),
							0, FastUtils.sin(radian1) * (radius + radiusSpacing));
						vec = VectorUtils.rotateZAxis(vec, angle);
						vec = VectorUtils.rotateXAxis(vec, finalLoc.getPitch() + extraPitch);
						vec = VectorUtils.rotateYAxis(vec, finalLoc.getYaw() + extraYaw);

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
	 * Use {@link com.playmonumenta.plugins.particle.PPParametric} if possible. Draw a particle curve in a vector space with parametric equations.
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
	 * Use {@link com.playmonumenta.plugins.particle.PPParametric} if possible.
	 * Draw a particle curve in a vector space with parametric equations,
	 * and the vector space is generated by front direction vector.
	 */
	public static void drawCurve(Location center, int paraMin, int paraMax, Vector front,
								 ParametricEquation f, ParametricEquation u, ParametricEquation r,
								 ParametricParticle pp) {
		Vector right = VectorUtils.rotateTargetDirection(front.clone(), 90, 0);
		Vector up = VectorUtils.rotateTargetDirection(front.clone(), 0, -90);

		drawCurve(center, paraMin, paraMax, front, up, right, f, u, r, pp);
	}

	/**
	 * Use {@link com.playmonumenta.plugins.particle.PPParametric} if possible.
	 * Draw a particle curve in a vector space with parametric equations,
	 * and the vector space is generated by yaw and pitch.
	 */
	public static void drawCurve(Location center, int paraMin, int paraMax, double yaw, double pitch,
								 ParametricEquation f, ParametricEquation u, ParametricEquation r,
								 ParametricParticle pp) {
		Vector front = VectorUtils.rotationToVector(yaw, pitch);
		Vector up = VectorUtils.rotationToVector(yaw, pitch - 90);
		Vector right = front.getCrossProduct(up).normalize();

		drawCurve(center, paraMin, paraMax, front, up, right, f, u, r, pp);
	}

	/**
	 * Use {@link PPCircle} if possible.
	 * Draw a ring with particles
	 *
	 * @param center Center location of the ring
	 * @param units  How many units consisting the ring
	 * @param normal Normal vector of the plane where the ring is
	 * @param radius Radius of the ring
	 * @param pp     Particle action at each unit
	 */
	public static void drawRing(Location center, int units, Vector normal, double radius, ParametricParticle pp) {
		drawCurve(center, 1, units, normal.clone().normalize(),
				t -> 0,
				t -> FastUtils.sin(Math.PI * 2 * t / units) * radius,
				t -> FastUtils.cos(Math.PI * 2 * t / units) * radius,
				pp
		);
	}

	public static void drawSphere(Location center, int units, double radius, ParametricParticle pp) {
		double thetaStep = Math.PI * 2 / units;
		Vector normal = new Vector(0, 1, 0);
		for (int i = 0; i < units; i++) {
			double theta = i * thetaStep;
			drawCurve(center, 1, units, normal.clone().normalize(),
					t -> radius * FastUtils.cos(theta) * FastUtils.sin(Math.PI * 2 * t / units),
					t -> radius * FastUtils.cos(Math.PI * 2 * t / units),
					t -> radius * FastUtils.sin(theta) * FastUtils.sin(Math.PI * 2 * t / units),
					pp
			);
		}
	}

	public static void drawDome(Location center, int units, double radius, Vector normal, ParametricParticle pp) {
		double thetaStep = Math.PI * 2 / units;
		Vector normalized = normal.clone().normalize();

		double[] rotation = VectorUtils.vectorToRotation(normalized);

		for (int i = 0; i < units; i++) {
			double theta = i * thetaStep;
			drawCurve(center, 0, units, rotation[0], rotation[1] + 90,
					t -> radius * FastUtils.cos(theta) * FastUtils.sin(Math.PI * t / units / 2),
					t -> radius * FastUtils.cos(Math.PI * t / units / 2),
					t -> radius * FastUtils.sin(theta) * FastUtils.sin(Math.PI * t / units / 2),
					pp
			);
		}
	}

	public static void drawSevenSegmentNumber(int number, Location center, Player player, double scale, double spacing, Particle particle, @Nullable Object data) {
		// Each digit takes up (scale + spacing) space.
		double digitSpace = scale + spacing;
		char[] digits = Integer.toString(number).toCharArray();
		int digitCount = digits.length;
		double leftMost;

		if (digitCount % 2 == 0) {
			// If there is an even number of digits, the leftmost digit's center is offset by
			// leftMost = (1/2 + digits/2) * digitSpace
			leftMost = (0.5 + Math.floor(digitCount / 2.0)) * digitSpace;
		} else {
			// If there is an odd number of digits, the leftmost digit's center is offset by
			// leftMost = (digits / 2 + 1) * digitSpace
			leftMost = (Math.floor(digitCount / 2.0) + 1) * digitSpace;
		}
		// The number has to be facing the specified player. The digits are placed along the perpendicular vector.
		Vector front = LocationUtils.getDirectionTo(player.getLocation(), center).setY(0).normalize();
		Vector up = new Vector(0, 1, 0);
		Vector right = VectorUtils.crossProd(up, front);

		// Then, each digit's center is found by the iteration:
		// leftMost + digitSpace * i, i = 0, i < digitCount
		Location currentDigitCenter = center.clone().subtract(right.clone().multiply(leftMost));
		for (char digit : digits) {
			int currentNumber = Integer.parseInt(String.valueOf(digit));
			currentDigitCenter.add(right.clone().multiply(digitSpace));
			drawSevenSegmentDigit(currentNumber, currentDigitCenter.clone(), player, scale, up, right, particle, data);
		}
	}

	public static void drawSevenSegmentDigit(int number, Location center, Player player, double scale, Particle particle, @Nullable Object data) {
		// Draw the number facing the specified player.
		Vector front = LocationUtils.getDirectionTo(player.getLocation(), center).setY(0).normalize();
		Vector up = new Vector(0, 1, 0);
		Vector right = VectorUtils.crossProd(up, front);

		drawSevenSegmentDigit(number, center, player, scale, up, right, particle, data);
	}

	public static void drawSevenSegmentDigit(int number, Location center, Player player, double scale, Vector up, Vector right, Particle particle, @Nullable Object data) {
		// Scale down the number
		Vector eUp = up.clone().multiply(scale);
		Vector eRight = right.clone().multiply(0.5).multiply(scale);

		PPLine top = new PPLine(particle, center.clone().add(eUp).subtract(eRight), center.clone().add(eUp).add(eRight))
			.countPerMeter(4);
		PPLine middle = new PPLine(particle, center.clone().subtract(eRight), center.clone().add(eRight))
			.countPerMeter(4);
		PPLine bottom = new PPLine(particle, center.clone().subtract(eUp).subtract(eRight), center.clone().subtract(eUp).add(eRight))
			.countPerMeter(4);

		PPLine topLeft = new PPLine(particle, center.clone().add(eUp).subtract(eRight), center.clone().subtract(eRight))
			.countPerMeter(4);
		PPLine topRight = new PPLine(particle, center.clone().add(eUp).add(eRight), center.clone().add(eRight))
			.countPerMeter(4);

		PPLine bottomLeft = new PPLine(particle, center.clone().subtract(eUp).subtract(eRight), center.clone().subtract(eRight))
			.countPerMeter(4);
		PPLine bottomRight = new PPLine(particle, center.clone().subtract(eUp).add(eRight), center.clone().add(eRight))
			.countPerMeter(4);

		if (data != null) {
			top.data(data);
			middle.data(data);
			bottom.data(data);
			topLeft.data(data);
			topRight.data(data);
			bottomLeft.data(data);
			bottomRight.data(data);
		}

		List<PPLine> linesToDraw;

		switch (number) {
			case 0 -> linesToDraw = List.of(top, topLeft, topRight, bottom, bottomLeft, bottomRight);
			case 1 -> linesToDraw = List.of(topRight, bottomRight);
			case 2 -> linesToDraw = List.of(top, topRight, middle, bottom, bottomLeft);
			case 3 -> linesToDraw = List.of(top, topRight, middle, bottom, bottomRight);
			case 4 -> linesToDraw = List.of(topLeft, topRight, middle, bottomRight);
			case 5 -> linesToDraw = List.of(top, topLeft, middle, bottom, bottomRight);
			case 6 -> linesToDraw = List.of(top, topLeft, middle, bottom, bottomLeft, bottomRight);
			case 7 -> linesToDraw = List.of(top, topRight, bottomRight);
			case 8 -> linesToDraw = List.of(top, topLeft, topRight, middle, bottom, bottomLeft, bottomRight);
			default -> linesToDraw = List.of(top, topLeft, topRight, middle, bottomRight);
		}

		linesToDraw.forEach(l -> l.spawnForPlayer(ParticleCategory.FULL, player));
	}

	public static void drawLine(Location start, Location end, int units, ParametricParticle pp) {
		Location current = start.clone();
		Vector direction = end.clone().toVector().subtract(start.toVector());
		Vector increment = direction.divide(new Vector(units, units, units));
		for (int t = 0; t <= units; t++) {
			pp.run(current, t);
			current = current.add(increment);
		}
	}

	public static void drawPerimeter(Location[] points, int units, boolean connect, ParametricParticle pp) {
		for (int i = 0; i < points.length - 1; i++) {
			drawLine(points[i], points[i + 1], units, pp);
		}
		if (connect) {
			drawLine(points[0], points[points.length - 1], units, pp);
		}
	}

	public static void drawRectangleTelegraph(Location start, double dx, double dz, int units, int pulses, int telegraphDuration, double baseSpeed, Particle particle, Plugin plugin, LivingEntity entity) {
		drawRectangleTelegraph(start, dx, dz, units, pulses, telegraphDuration, 0, baseSpeed, particle, plugin, entity);
	}

	public static void drawRectangleTelegraph(Location start, double dx, double dz, int units, int pulses, int telegraphDuration, int pulseStartDelay, double baseSpeed, Particle particle, Plugin plugin, LivingEntity entity) {
		// Get the particles out of the ground
		Location adjustedStart = start.clone().add(0, 0.1, 0);
		Location[] corners = {
			adjustedStart,
			adjustedStart.clone().add(dx, 0, 0),
			adjustedStart.clone().add(dx, 0, dz),
			adjustedStart.clone().add(0, 0, dz)
		};
		Location center = adjustedStart.clone().add(dx / 2, 0, dz / 2);
		double pulseDelay = (double) (telegraphDuration - pulseStartDelay) / pulses;

		Player eventualPlayer = null;
		if (entity instanceof Player player) {
			eventualPlayer = player;
		}
		Player finalPlayer = eventualPlayer;

		long borderDelay = (pulseStartDelay > 0) ? (long) (pulseDelay / 2) : (long) pulseDelay;

		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				// Border, starting immediately
				drawPerimeter(corners, units, true,
					(l, t) -> {
						PartialParticle pp = new PartialParticle(particle, l, 1).extra(0);
						if (finalPlayer != null) {
							pp.spawnAsPlayerActive(finalPlayer);
						} else {
							pp.spawnAsEntityActive(entity);
						}
					}
				);
			}
		};
		runnable.runTaskTimer(plugin, 0, borderDelay);

		new BukkitRunnable() {
			int mTimesRun = 0;
			@Override
			public void run() {
				// Pulses, starting after startDelay
				drawPerimeter(corners, units, true,
					(l, t) -> {
						Vector direction = center.toVector().subtract(l.toVector()).normalize();
						double speedMod = center.distance(l) / 2;
						PartialParticle pp = new PartialParticle(particle, l, 1).extra(baseSpeed * speedMod).directionalMode(true)
							.delta(direction.getX(), direction.getY(), direction.getZ());
						if (finalPlayer != null) {
							pp.spawnAsPlayerActive(finalPlayer);
						} else {
							pp.spawnAsEntityActive(entity);
						}
					}
				);

				mTimesRun++;
				if (pulses == 0 || mTimesRun == pulses) {
					this.cancel();
					runnable.cancel();
				}
			}
		}.runTaskTimer(plugin, pulseStartDelay, (long) pulseDelay);
	}

	public static void drawRectangleTelegraphAnimation(Location start, double dx, double dz, int units, int pulses, int animationTimes, int telegraphDuration, Particle particle, Object data, Plugin plugin, LivingEntity entity) {
		// Get the particles out of the ground
		Location adjustedStart = start.clone().add(0, 0.1, 0);
		Location[] corners = {
			adjustedStart.clone(),
			adjustedStart.clone().add(dx, 0, 0),
			adjustedStart.clone().add(dx, 0, dz),
			adjustedStart.clone().add(0, 0, dz)
		};

		int finalAnimationTimes = (animationTimes > 0) ? animationTimes : 1;

		double pulseDelay = (double) telegraphDuration / pulses / finalAnimationTimes;

		double xIncrease = dx / 2 / pulses;
		double zIncrease = dz / 2 / pulses;

		Player eventualPlayer = null;
		if (entity instanceof Player player) {
			eventualPlayer = player;
		}
		Player finalPlayer = eventualPlayer;

		new BukkitRunnable() {

			int mTimesAnimated = 0;
			@Override
			public void run() {
				Location[] innerCorners = {
					adjustedStart.clone(),
					adjustedStart.clone().add(dx, 0, 0),
					adjustedStart.clone().add(dx, 0, dz),
					adjustedStart.clone().add(0, 0, dz)
				};
				new BukkitRunnable() {
					int mTimesRun = 0;
					@Override
					public void run() {
						// Outline always visible
						drawPerimeter(corners, units, true,
							(l, t) -> {
								PartialParticle pp = new PartialParticle(particle, l, 1).extra(0).data(data);
								if (finalPlayer != null) {
									pp.spawnAsPlayerActive(finalPlayer);
								} else {
									pp.spawnAsEntityActive(entity);
								}
							}
						);
						// Inner shrinking rectangles
						drawPerimeter(innerCorners, units, true,
							(l, t) -> {
								PartialParticle pp = new PartialParticle(particle, l, 1).extra(0).data(data);
								if (finalPlayer != null) {
									pp.spawnAsPlayerActive(finalPlayer);
								} else {
									pp.spawnAsEntityActive(entity);
								}
							}
						);

						innerCorners[0].add(xIncrease, 0, zIncrease);
						innerCorners[1].add(-xIncrease, 0, zIncrease);
						innerCorners[2].add(-xIncrease, 0, -zIncrease);
						innerCorners[3].add(xIncrease, 0, -zIncrease);

						mTimesRun++;
						if (pulses == 0 || mTimesRun == pulses) {
							this.cancel();
						}
					}
				}.runTaskTimer(plugin, 0, (long) pulseDelay);

				mTimesAnimated++;
				if (mTimesAnimated == finalAnimationTimes) {
					this.cancel();
				}
			}
		}.runTaskTimer(plugin, 0, telegraphDuration / animationTimes);
	}

	public static void drawCircleTelegraph(Location center, double radius, int units, int pulses, int telegraphDuration, double baseSpeed, boolean hideMiddleCircle, Particle particle, Plugin plugin, LivingEntity entity) {
		// Get the particles out of the ground
		double pulseDelay = (double) telegraphDuration / pulses;
		Vector normal = new Vector(0, 1, 0);
		Location adjustedCenter = center.clone().add(0, 0.1, 0);

		Player eventualPlayer = null;
		if (entity instanceof Player player) {
			eventualPlayer = player;
		}
		Player finalPlayer = eventualPlayer;

		new BukkitRunnable() {
			int mTimesRun = 0;
			@Override
			public void run() {
				if (!hideMiddleCircle) {
					drawRing(adjustedCenter, units, normal, radius,
						(l, t) -> {
							PartialParticle pp = new PartialParticle(particle, l, 1).extra(0);
							if (finalPlayer != null) {
								pp.spawnAsPlayerActive(finalPlayer);
							} else {
								pp.spawnAsEntityActive(entity);
							}
						}
					);
				}
				drawRing(adjustedCenter, units, normal, radius,
					(l, t) -> {
						Vector direction = adjustedCenter.toVector().subtract(l.toVector()).normalize();
						PartialParticle pp = new PartialParticle(particle, l, 1).extra(baseSpeed).directionalMode(true)
							.delta(direction.getX(), direction.getY(), direction.getZ());
						if (finalPlayer != null) {
							pp.spawnAsPlayerActive(finalPlayer);
						} else {
							pp.spawnAsEntityActive(entity);
						}
					}
				);

				mTimesRun++;
				if (pulses == 0 || mTimesRun == pulses) {
					this.cancel();
				}
			}
		}.runTaskTimer(plugin, 0, (long) pulseDelay);
	}

	public static BukkitRunnable drawFlowerPattern(Location center, double radius, int petals, int duration, double thetaOffset,
												   double thetaIncrement, float baseSpeed, Particle particle, Entity spawnerEntity) {
		double pointOffset = Math.PI * 2 / petals;
		BukkitRunnable flowerRunnable = new BukkitRunnable() {
			int mTicks = 0;
			double mThetaForward = thetaOffset;
			double mThetaBackward = thetaOffset;
			final PPCircle mCircle = new PPCircle(particle, center.clone(), radius).ringMode(true);

			@Override
			public void run() {
				for (int i = 0; i < petals; i++) {
					double forwardAngle = mThetaForward + i * pointOffset;
					double backwardAngle = mThetaBackward + i * pointOffset;
					Location forwardLoc = center.clone().add(FastUtils.cos(forwardAngle) * radius, 0, FastUtils.sin(forwardAngle) * radius);
					Vector toCenterForward = forwardLoc.toVector().subtract(center.toVector()).normalize();
					Location backwardLoc = center.clone().add(FastUtils.cos(backwardAngle) * radius, 0, FastUtils.sin(backwardAngle) * radius);
					Vector toCenterBackward = backwardLoc.toVector().subtract(center.toVector()).normalize();

					// Particle going forwards, shooting towards the center
					PartialParticle forward = new PartialParticle(particle, forwardLoc, 1).extra(0).directionalMode(true)
						.delta(toCenterForward.getX(), toCenterForward.getY(), toCenterForward.getZ())
						.extra(baseSpeed).distanceFalloff(radius * 24);
					// Particle going backwards, shooting towards the center
					PartialParticle backward = new PartialParticle(particle, backwardLoc, 1).extra(0).directionalMode(true)
						.delta(toCenterBackward.getX(), toCenterBackward.getY(), toCenterBackward.getZ())
						.extra(baseSpeed).distanceFalloff(radius * 24);

					if (spawnerEntity instanceof Player player) {
						// Particle Base Circle
						if (mTicks % 10 == 0) {
							mCircle.spawnAsPlayerActive(player);
						}
						forward.spawnAsPlayerActive(player);
						backward.spawnAsPlayerActive(player);
					} else {
						// Particle Base Circle
						if (mTicks % 10 == 0) {
							mCircle.spawnAsEntityActive(spawnerEntity);
						}
						forward.spawnAsEntityActive(spawnerEntity);
						backward.spawnAsEntityActive(spawnerEntity);
					}
				}

				if (mTicks >= duration) {
					this.cancel();
				}
				mThetaForward += thetaIncrement;
				mThetaBackward -= thetaIncrement;
				mTicks++;
			}
		};
		flowerRunnable.runTaskTimer(com.playmonumenta.plugins.Plugin.getInstance(), 0, 1);
		return flowerRunnable;
	}

	@Deprecated
	public static void instantlyDrawFlowerPattern(Location center, Vector normal, double radius, int petals, double thetaStep, PreciseParametricParticle pp) {
		if (petals < 3) {
			return;
		}

		drawPreciseCurve(center, 0, Math.PI * (petals - 2), thetaStep, normal.clone().normalize(),
			t -> 0,
			t -> radius * FastUtils.cos((double) petals / ((double) petals - 2) * t) * FastUtils.sin(t),
			t -> radius * FastUtils.cos((double) petals / ((double) petals - 2) * t) * FastUtils.cos(t),
			pp
		);
	}

	public static void drawWeirdCirclePattern(Location center, Vector normal, double radius, int complexity, double thetaStep, PreciseParametricParticle pp) {

		int actualComplexity = (complexity % 2 == 0) ? complexity + 1 : complexity;

		drawPreciseCurve(center, 0, Math.PI * actualComplexity * 2, thetaStep, normal.clone().normalize(),
			t -> 0,
			t -> radius * FastUtils.cos(2.0 / (double) actualComplexity * t) * FastUtils.sin(t),
			t -> radius * FastUtils.cos(2.0 / (double) actualComplexity * t) * FastUtils.cos(t),
			pp
		);
	}

	@Deprecated
	public static void drawSharpPetalFlower(Location center, Vector normal, double radius, int petals, double thetaStep, PreciseParametricParticle pp) {
		if (petals < 3) {
			return;
		}

		int actualPetals = (petals % 2 == 0) ? 2 * petals : petals;

		// maxRadius * (1 - abs(cos(n/4) * t)))

		drawPreciseCurve(center, 0, Math.PI * 4, thetaStep, normal.clone().normalize(),
			t -> 0,
			t -> radius * (1 - Math.abs(FastUtils.cos((double) actualPetals / 4.0 * t))) * FastUtils.sin(t),
			t -> radius * (1 - Math.abs(FastUtils.cos((double) actualPetals / 4.0 * t))) * FastUtils.cos(t),
			pp
		);
	}

	public static void drawPreciseCurve(Location center, double paraMin, double paraMax, double thetaStep,
										Vector e1, Vector e2, Vector e3,
										PreciseParametricEquation eq1, PreciseParametricEquation eq2, PreciseParametricEquation eq3,
										PreciseParametricParticle pp) {
		Location loc;
		for (double t = paraMin; t <= paraMax; t += thetaStep) {
			loc = center.clone();
			loc.add(e1.clone().multiply(eq1.equation(t)));
			loc.add(e2.clone().multiply(eq2.equation(t)));
			loc.add(e3.clone().multiply(eq3.equation(t)));
			pp.run(loc, t);
		}
	}

	public static void drawPreciseCurve(Location center, double paraMin, double paraMax, double thetaStep, Vector front,
										PreciseParametricEquation f, PreciseParametricEquation u, PreciseParametricEquation r,
										PreciseParametricParticle pp) {
		Vector right = VectorUtils.rotateTargetDirection(front.clone(), 90, 0);
		Vector up = VectorUtils.rotateTargetDirection(front.clone(), 0, -90);

		drawPreciseCurve(center, paraMin, paraMax, thetaStep, front, up, right, f, u, r, pp);
	}

	/**
	 * Adaptation of Touch Of Entropy's code for orbs.
	*/
	public static void launchOrb(Vector initialDir, Location startLoc, LivingEntity launcher, LivingEntity target, int expireTime,
								 @Nullable Location targetLoc, Particle.DustOptions dustOptions, Consumer<LivingEntity> hitAction) {
		new BukkitRunnable() {
			final Location mCurrLoc = startLoc.clone();
			int mTicks = 0;
			double mArcCurve = 0;
			Vector mCurrDir = initialDir.clone();

			@Override
			public void run() {
				mTicks++;

				Location to = targetLoc != null ? targetLoc : LocationUtils.getHalfHeightLocation(target);

				for (int i = 0; i < 4; i++) {
					if (mTicks <= 2) {
						mCurrDir = initialDir.clone();
					} else {
						mArcCurve += 0.085;
						mCurrDir = initialDir.clone().add(LocationUtils.getDirectionTo(to, mCurrLoc).multiply(mArcCurve));
					}

					if (mCurrDir.length() > 0.2) {
						mCurrDir.normalize().multiply(0.2);
					}

					mCurrLoc.add(mCurrDir);
					spawnParticleAsLivingEntity(new PartialParticle(Particle.REDSTONE, mCurrLoc, 1).data(dustOptions), launcher);

					if (mTicks > 5 && mCurrLoc.distance(to) < 0.35) {
						hitAction.accept(target);
						this.cancel();
						return;
					}
				}

				if (mTicks >= expireTime) {
					this.cancel();
				}
			}

		}.runTaskTimer(com.playmonumenta.plugins.Plugin.getInstance(), 0, 1);
	}

	public static void spawnParticleAsLivingEntity(PartialParticle pp, LivingEntity living) {
		if (living instanceof Player playerLiving) {
			pp.spawnAsPlayerActive(playerLiving);
		} else {
			pp.spawnAsEntityActive(living);
		}
	}

	public static Particle.DustOptions getRandomColorOptions(float size) {
		return new Particle.DustOptions(Color.fromRGB(FastUtils.randomIntInRange(0, 255), FastUtils.randomIntInRange(0, 255), FastUtils.randomIntInRange(0, 255)), size);
	}

	public static Particle.DustOptions getRandomColorOptions(int min, float size) {
		return new Particle.DustOptions(Color.fromRGB(FastUtils.randomIntInRange(min, 255), FastUtils.randomIntInRange(min, 255), FastUtils.randomIntInRange(min, 255)), size);
	}

}
