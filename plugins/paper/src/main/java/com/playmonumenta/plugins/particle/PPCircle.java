package com.playmonumenta.plugins.particle;

import com.destroystokyo.paper.ParticleBuilder;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Location;
import org.bukkit.Particle;


public class PPCircle extends AbstractPartialParticle<PPCircle> {
	/*
	 * Radius of the circle within which all particles must
	 * start, prior to applying deltas (which may put them outside).
	 */
	protected double mRadius;

	/*
	 * Set to true to evenly spread particles in a ring along the circle's
	 * circumference.
	 * Set to false to randomise particles within the circle's entire area.
	 */
	protected boolean mRingMode = false;

	/*-------------------------------------------------------------------------------
	 * Constructors
	 */

	public PPCircle(Particle particle, Location centerLocation, double radius) {
		super(particle, centerLocation);
		mRadius = radius;
	}

	/*-------------------------------------------------------------------------------
	 * Parameter getters and setters
	 */

	/**
	 * Sets particles to spawn in a ring around the circle (true) or the circle's area (false)
	 */
	public PPCircle ringMode(boolean ringMode) {
		mRingMode = ringMode;
		return this;
	}

	/**
	 * Gets whether particles spawn in a ring around the circle (true) or the circle's area (false)
	 */
	public boolean ringMode() {
		return mRingMode;
	}

	public PPCircle radius(double radius) {
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
				currentDegrees += 1.0 * revolutionDegrees / partialCount;
			}

			double offsetX = FastUtils.sinDeg(currentDegrees) * mRadius;
			double offsetZ = FastUtils.cosDeg(currentDegrees) * mRadius;
			if (!mRingMode) {
				// Randomly move inwards
				double inwardFactor = Math.sqrt(FastUtils.RANDOM.nextDouble());
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
