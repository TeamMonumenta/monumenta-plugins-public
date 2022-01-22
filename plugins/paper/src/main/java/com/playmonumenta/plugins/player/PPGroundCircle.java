package com.playmonumenta.plugins.player;

import com.destroystokyo.paper.ParticleBuilder;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.checkerframework.checker.nullness.qual.Nullable;



public class PPGroundCircle extends PartialParticle {
	/*
	 * Radius of the circle flat on the ground within which all particles must
	 * start, prior to applying deltas (which may put them outside).
	 */
	protected double mRadius;

	/*
	 * Set to true to evenly spread particles in a ring along the circle's
	 * circumference.
	 * Set to false to randomise particles within the circle's entire area.
	 */
	protected boolean mRingMode;

	/*-------------------------------------------------------------------------------
	 * Constructors
	 */

	public PPGroundCircle(Particle particle, Location centerLocation, int count, double delta, double extra) {
		super(particle, centerLocation, count, delta, extra);
	}

	public PPGroundCircle(Particle particle, Location centerLocation, int count, double delta, double extra, @Nullable Object data) {
		super(particle, centerLocation, count, delta, extra, data);
	}

	public PPGroundCircle(Particle particle, Location centerLocation, int count, double delta, double extra, @Nullable Object data, boolean directionalMode, double extraVariance) {
		super(particle, centerLocation, count, delta, extra, data, directionalMode, extraVariance);
	}

	public PPGroundCircle(Particle particle, Location centerLocation, int count, double delta, double extra, @Nullable Object data, boolean directionalMode, double extraVariance, boolean minimumMultiplier) {
		super(particle, centerLocation, count, delta, extra, data, directionalMode, extraVariance, minimumMultiplier);
	}

	public PPGroundCircle(Particle particle, Location centerLocation, int count, double deltaX, double deltaY, double deltaZ, double extra) {
		super(particle, centerLocation, count, deltaX, deltaY, deltaZ, extra);
	}

	public PPGroundCircle(Particle particle, Location centerLocation, int count, double deltaX, double deltaY, double deltaZ, double extra, @Nullable Object data) {
		super(particle, centerLocation, count, deltaX, deltaY, deltaZ, extra, data);
	}

	public PPGroundCircle(Particle particle, Location centerLocation, int count, double deltaX, double deltaY, double deltaZ, double extra, @Nullable Object data, boolean directionalMode, double extraVariance) {
		super(particle, centerLocation, count, deltaX, deltaY, deltaZ, extra, data, directionalMode, extraVariance);
	}

	public PPGroundCircle(Particle particle, Location centerLocation, int count, double deltaX, double deltaY, double deltaZ, double extra, @Nullable Object data, boolean directionalMode, double extraVariance, boolean minimumMultiplier) {
		super(particle, centerLocation, count, deltaX, deltaY, deltaZ, extra, data, directionalMode, extraVariance, minimumMultiplier);
	}

	/*-------------------------------------------------------------------------------
	 * Required init methods
	 * One of these must be called prior to spawning this particle
	 */

	/**
	 * Use default ringMode.
	 */
	public PPGroundCircle init(double radius) {
		return init(radius, false);
	}

	/**
	 * Define attributes specific to this subclass of PartialParticle.
	 */
	public PPGroundCircle init(double radius, boolean ringMode) {
		ringMode(ringMode);
		radius(radius);
		return this;
	}

	/*-------------------------------------------------------------------------------
	 * Parameter getters and setters
	 */

	/** Sets particles to spawn in a ring around the circle (true) or the circle's area (false) */
	public PPGroundCircle ringMode(boolean ringMode) {
		mRingMode = ringMode;
		return this;
	}

	/** Gets whether particles spawn in a ring around the circle (true) or the circle's area (false) */
	public boolean ringMode() {
		return mRingMode;
	}

	public PPGroundCircle radius(double radius) {
		mRadius = radius;
		return this;
	}

	public double radius() {
		return mRadius;
	}

	/*-------------------------------------------------------------------------------
	 * Methods
	 */

	@Override
	protected void doSpawn(ParticleBuilder packagedValues) {
		int partialCount = packagedValues.count();
		Location centerLocation = packagedValues.location();
		// Spawning one by one, looping manually by partialCount times.
		// spawnWithSettings() will handle whether count should be 0 for
		// directional mode
		packagedValues.count(1);

		int revolutionDegrees = 360;
		double currentDegrees = 0;
		if (mRingMode) {
			// Randomly rotated starting offset
			currentDegrees = FastUtils.randomDoubleInRange(0, revolutionDegrees);
		}

		for (int i = 0; i < partialCount; i++) {
			if (!mRingMode) {
				// Always rerandomise rotation
				currentDegrees = FastUtils.randomDoubleInRange(0, revolutionDegrees);
			} else {
				// Add on after initial random rotation
				currentDegrees += revolutionDegrees / partialCount;
			}

			double offsetX = FastUtils.sinDeg(currentDegrees) * mRadius;
			double offsetZ = FastUtils.cosDeg(currentDegrees) * mRadius;
			if (!mRingMode) {
				// Randomly move inwards
				double inwardFactor = FastUtils.RANDOM.nextDouble();
				offsetX *= inwardFactor;
				offsetZ *= inwardFactor;
			}

			Location currentLocation = centerLocation.clone();
			currentLocation.add(offsetX, 0, offsetZ);
			packagedValues.location(currentLocation);

			spawnUsingSettings(packagedValues);
		}
	}
}
