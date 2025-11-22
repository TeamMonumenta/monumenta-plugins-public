package com.playmonumenta.plugins.particle;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Objects;
import java.util.function.DoublePredicate;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link PartialParticle} that draws a circle or ring of particles.
 */
public class PPCircle extends AbstractPartialParticle<PPCircle> {
	/*
	 * Radius of the circle within which all particles must
	 * start, prior to applying deltas (which may put them outside).
	 */
	protected double mRadius;

	protected double mInnerRadiusFactor = 1;

	private double mParticlesPerMeter = -1;
	private double mMinParticlesPerMeter = 0;
	protected double mStartAngleDeg = 0;
	protected double mEndAngleDeg = 360;
	private double mOffset = 0;
	private boolean mIncludeStart = true;
	private boolean mIncludeEnd = false;
	private int mTicks = 0;

	protected Vector mAxis1 = new Vector(1, 0, 0);
	protected Vector mAxis2 = new Vector(0, 0, 1);

	protected boolean mRotateDelta = false;

	protected boolean mRandomizeAngle = true;

	protected @Nullable DoublePredicate mAnglePredicate;

	/*-------------------------------------------------------------------------------
	 * Constructors
	 */

	public PPCircle(Particle particle, Location centerLocation, double radius) {
		super(particle, centerLocation);
		mRadius = radius;
	}

	@Override
	public PPCircle copy() {
		return copy(new PPCircle(mParticle, mLocation.clone(), mRadius));
	}

	@Override
	public PPCircle copy(PPCircle copy) {
		super.copy(copy);
		copy.mRadius = mRadius;
		copy.mInnerRadiusFactor = mInnerRadiusFactor;
		copy.mParticlesPerMeter = mParticlesPerMeter;
		copy.mMinParticlesPerMeter = mMinParticlesPerMeter;
		copy.mStartAngleDeg = mStartAngleDeg;
		copy.mEndAngleDeg = mEndAngleDeg;
		copy.mOffset = mOffset;
		copy.mIncludeStart = mIncludeStart;
		copy.mIncludeEnd = mIncludeEnd;
		copy.mTicks = mTicks;
		copy.mAxis1 = mAxis1.clone();
		copy.mAxis2 = mAxis2.clone();
		copy.mRotateDelta = mRotateDelta;
		copy.mRandomizeAngle = mRandomizeAngle;
		copy.mAnglePredicate = mAnglePredicate;
		return copy;
	}

	/*-------------------------------------------------------------------------------
	 * Parameter getters and setters
	 */

	/**
	 * Sets particles to spawn in a ring around the circle (true) or the circle's area (false)
	 *
	 * @see #innerRadiusFactor(double) Making a ring with nonzero thickness
	 */
	public PPCircle ringMode(boolean ringMode) {
		mInnerRadiusFactor = ringMode ? 1 : 0;
		return this;
	}

	/**
	 * Gets whether particles spawn in a ring around the circle (true) or the circle's area (false)
	 */
	public boolean ringMode() {
		return mInnerRadiusFactor == 1;
	}

	public PPCircle radius(double radius) {
		mRadius = radius;
		return this;
	}

	public double radius() {
		return mRadius;
	}

	public PPCircle innerRadiusFactor(double innerRadiusFactor) {
		mInnerRadiusFactor = innerRadiusFactor;
		return this;
	}

	public PPCircle ticks(int ticks) {
		mTicks = ticks;
		return this;
	}

	public int ticks() {
		return mTicks;
	}

	/**
	 * In ring mode, sets the number of particles so that there's the requested number of particles per meter.
	 * Also sets the minimum number per meter to 1/4th of this value.
	 */
	public PPCircle countPerMeter(double countPerMeter) {
		mParticlesPerMeter = countPerMeter;
		mMinParticlesPerMeter = countPerMeter / 4;
		return this;
	}

	public PPCircle minimumCountPerMeter(double minimumCountPerMeter) {
		mMinParticlesPerMeter = minimumCountPerMeter;
		return this;
	}

	public PPCircle includeStart(boolean includeStart) {
		mIncludeStart = includeStart;
		return this;
	}

	public PPCircle includeEnd(boolean includeEnd) {
		mIncludeEnd = includeEnd;
		return this;
	}

	/**
	 * Sets the orientation of this circle by defining which axes are used for the circle's plane.
	 * By not using perpendicular normalized vectors, ellipses can be made.
	 */
	public PPCircle axes(Vector axis1, Vector axis2) {
		mAxis1 = axis1;
		mAxis2 = axis2;
		return getSelf();
	}

	/**
	 * Limits the circle to the given degrees, starting at the x-axis and turning towards z.
	 * Turns off randomized angles when used, and enabled inclusion of both start and end if giving an arc &lt; 360°.
	 */
	public PPCircle arcDegree(double startAngleDeg, double endAngleDeg) {
		mStartAngleDeg = startAngleDeg;
		mEndAngleDeg = endAngleDeg;
		mRandomizeAngle = false;
		if (Math.abs(endAngleDeg - startAngleDeg) < 360) {
			mIncludeStart = true;
			mIncludeEnd = true;
		}
		return getSelf();
	}

	public PPCircle randomizeAngle(boolean randomizeAngle) {
		mRandomizeAngle = randomizeAngle;
		return getSelf();
	}

	/**
	 * Enables rotating the delta values for spawned particles.
	 * The values will be rotated around this circle's center, originating at rotation 0° (at the first axis; positive x by default)
	 */
	public PPCircle rotateDelta(boolean rotateDelta) {
		mRotateDelta = rotateDelta;
		return getSelf();
	}

	/**
	 * Add a predicate for whether particles should be spawned for a given angle.
	 * The given predicate will be called with an angle in degrees, as an absolute rotation.
	 */
	public PPCircle anglePredicate(DoublePredicate anglePredicate) {
		mAnglePredicate = anglePredicate;
		return getSelf();
	}

	/**
	 * Offsets the spawned particles by a factor of the distance between particles.
	 * Useful when drawing the same circle multiple times over time, and wanting different particle locations each time.
	 */
	public PPCircle offset(double factor) {
		mOffset = factor;
		if (factor != 0) {
			mIncludeEnd = false;
		}
		return this;
	}

	/*-------------------------------------------------------------------------------
	 * Methods
	 */

	@Override
	protected int getPartialCount(double multiplier, Player player, ParticleCategory source) {
		if (mParticlesPerMeter < 0) {
			return super.getPartialCount(multiplier, player, source);
		}
		int count = Math.max(mMinimumCount, (int) Math.ceil(mRadius * Math.abs(Math.toRadians(mEndAngleDeg - mStartAngleDeg)) * Math.max(mParticlesPerMeter * multiplier, mMinParticlesPerMeter)));
		if (mIncludeStart && mIncludeEnd) {
			count++;
		} else if (!mIncludeStart && !mIncludeEnd && count > 1) {
			count--;
		}
		return count;
	}

	@Override
	protected void doSpawn(PartialParticleBuilder packagedValues) {
		int partialCount = packagedValues.count();
		Location centerLocation = Objects.requireNonNull(packagedValues.location());
		// Spawning one by one, looping manually by partialCount times.
		// spawnWithSettings() will handle whether count should be 0 for
		// directional mode
		packagedValues.count(1);

		double currentDegrees = mStartAngleDeg;
		if (mInnerRadiusFactor == 1 && mRandomizeAngle && (mTicks == 0)) {
			// Randomly rotated starting offset if enabled and making a ring
			currentDegrees = FastUtils.randomDoubleInRange(0, 360);
		}

		Vector normal = mAxis1.getCrossProduct(mAxis2); // note that this is -y for the default axes
		Vector originalDelta = new Vector(packagedValues.offsetX(), packagedValues.offsetY(), packagedValues.offsetZ());

		int rawCount = partialCount - (mIncludeStart && mIncludeEnd ? 1 : 0) + (!mIncludeStart && !mIncludeEnd ? 1 : 0);
		double step = (mEndAngleDeg - mStartAngleDeg) / Math.max(1, rawCount);

		if (mTicks > 0) {
			new BukkitRunnable() {
				int mSpawned = 0;
				int mCurrentTick = 0;

				@Override
				public void run() {
					if (mSpawned >= partialCount || mCurrentTick >= mTicks) {
						cancel();
						return;
					}
					// Evenly distributes total particles across entire duration.
					// Calculates floor of average per-tick value, then spawns one extra depending on fractional part
					double perTickF = (double) partialCount / mTicks;
					int toSpawn = (int) Math.floor(perTickF);
					if (Math.random() < (perTickF - toSpawn)) {
						toSpawn++;
					}
					for (int i = 0; i < toSpawn && mSpawned < partialCount; i++, mSpawned++) {
						double currentDegrees = mStartAngleDeg + step * mSpawned + step * mOffset;
						double rad = Math.toRadians(currentDegrees);

						if (!mIncludeStart && mSpawned == 0) {
							continue;
						}
						if (!mIncludeEnd && mSpawned == partialCount - 1) {
							continue;
						}
						if (mAnglePredicate != null && !mAnglePredicate.test(currentDegrees)) {
							continue;
						}

						double x = FastUtils.cos(rad) * mRadius;
						double z = FastUtils.sin(rad) * mRadius;
						double inwardFactor = 1;
						if (mInnerRadiusFactor < 1) {
							// Randomly move inwards
							inwardFactor = Math.sqrt(FastUtils.randomDoubleInRange(mInnerRadiusFactor * mInnerRadiusFactor, 1));
							x *= inwardFactor;
							z *= inwardFactor;
						}

						Location loc = centerLocation.clone()
							.add(mAxis1.clone().multiply(x))
							.add(mAxis2.clone().multiply(z));
						packagedValues.location(loc);

						if (mRotateDelta) {
							Vector rotatedDelta = originalDelta.clone().rotateAroundNonUnitAxis(normal, rad).multiply(inwardFactor);
							packagedValues.offset(rotatedDelta.getX(), rotatedDelta.getY(), rotatedDelta.getZ());
						}

						spawnUsingSettings(packagedValues);
					}
					mCurrentTick++;
				}
			}.runTaskTimerAsynchronously(Plugin.getInstance(), 0L, 1L);
		} else {
			currentDegrees += mOffset * step;

			for (int i = 0; i < partialCount; i++) {
				if (mInnerRadiusFactor != 1 && mRandomizeAngle) {
					// If enabled, re-randomise rotation for each particle when making a filled circle
					currentDegrees = FastUtils.randomDoubleInRange(mStartAngleDeg, mEndAngleDeg);
				} else if (i > 0 || !mIncludeStart) {
					// Add on after initial rotation
					currentDegrees += step;
				}
				if (mAnglePredicate != null && !mAnglePredicate.test(currentDegrees)) {
					continue;
				}

				double offsetX = FastUtils.cosDeg(currentDegrees) * mRadius;
				double offsetZ = FastUtils.sinDeg(currentDegrees) * mRadius;
				double inwardFactor = 1;
				if (mInnerRadiusFactor < 1) {
					// Randomly move inwards
					inwardFactor = Math.sqrt(FastUtils.randomDoubleInRange(mInnerRadiusFactor * mInnerRadiusFactor, 1));
					offsetX *= inwardFactor;
					offsetZ *= inwardFactor;
				}

				packagedValues.location(centerLocation.clone().add(mAxis1.clone().multiply(offsetX)).add(mAxis2.clone().multiply(offsetZ)));

				if (mRotateDelta) {
					Vector rotatedDelta = originalDelta.clone().rotateAroundNonUnitAxis(normal, Math.toRadians(currentDegrees)).multiply(inwardFactor);
					packagedValues.offset(rotatedDelta.getX(), rotatedDelta.getY(), rotatedDelta.getZ());
				}

				spawnUsingSettings(packagedValues);
			}
		}
	}
}
