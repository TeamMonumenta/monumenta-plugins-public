package com.playmonumenta.plugins.player;

import com.destroystokyo.paper.ParticleBuilder;
import com.playmonumenta.plugins.utils.FastUtils;

import org.bukkit.Location;
import org.bukkit.Particle;

import org.checkerframework.checker.nullness.qual.Nullable;



public class PPPillar extends PartialParticle {
	/*
	 * Height of the pillar above mLocation within which to equally randomly
	 * (as opposed to normal Gaussian distribution about mLocation)
	 * spread particles, prior to applying deltas
	 * (which may put them above or below).
	 */
	protected double mHeight;

	/*-------------------------------------------------------------------------------
	 * Constructors
	 */

	public PPPillar(Particle particle, Location baseLocation, int count, double delta, double extra) {
		super(particle, baseLocation, count, delta, extra);
	}

	public PPPillar(Particle particle, Location baseLocation, int count, double delta, double extra, @Nullable Object data) {
		super(particle, baseLocation, count, delta, extra, data);
	}

	public PPPillar(Particle particle, Location baseLocation, int count, double delta, double extra, @Nullable Object data, boolean directionalMode, double extraVariance) {
		super(particle, baseLocation, count, delta, extra, data, directionalMode, extraVariance);
	}

	public PPPillar(Particle particle, Location baseLocation, int count, double delta, double extra, @Nullable Object data, boolean directionalMode, double extraVariance, boolean minimumMultiplier) {
		super(particle, baseLocation, count, delta, extra, data, directionalMode, extraVariance, minimumMultiplier);
	}

	public PPPillar(Particle particle, Location baseLocation, int count, double deltaX, double deltaY, double deltaZ, double extra) {
		super(particle, baseLocation, count, deltaX, deltaY, deltaZ, extra);
	}

	public PPPillar(Particle particle, Location baseLocation, int count, double deltaX, double deltaY, double deltaZ, double extra, @Nullable Object data) {
		super(particle, baseLocation, count, deltaX, deltaY, deltaZ, extra, data);
	}

	public PPPillar(Particle particle, Location baseLocation, int count, double deltaX, double deltaY, double deltaZ, double extra, @Nullable Object data, boolean directionalMode, double extraVariance) {
		super(particle, baseLocation, count, deltaX, deltaY, deltaZ, extra, data, directionalMode, extraVariance);
	}

	public PPPillar(Particle particle, Location baseLocation, int count, double deltaX, double deltaY, double deltaZ, double extra, @Nullable Object data, boolean directionalMode, double extraVariance, boolean minimumMultiplier) {
		super(particle, baseLocation, count, deltaX, deltaY, deltaZ, extra, data, directionalMode, extraVariance, minimumMultiplier);
	}

	/*-------------------------------------------------------------------------------
	 * Required init methods
	 * One of these must be called prior to spawning this particle
	 */

	/*
	 * Define attributes specific to this subclass of PartialParticle.
	 */
	public PPPillar init(double height) {
		mHeight = height;

		return this;
	}

	/*-------------------------------------------------------------------------------
	 * Parameter getters and setters
	 */

	public PPPillar height(double height) {
		mHeight = height;
		return this;
	}

	public double height() {
		return mHeight;
	}

	/*-------------------------------------------------------------------------------
	 * Methods
	 */

	@Override
	protected void doSpawn(ParticleBuilder packagedValues) {
		int partialCount = packagedValues.count();
		Location baseLocation = packagedValues.location();
		packagedValues.count(1);

		for (int i = 0; i < partialCount; i++) {
			Location currentLocation = baseLocation.clone();
			currentLocation.setY(
				currentLocation.getY() + FastUtils.randomDoubleInRange(0, mHeight)
			);
			packagedValues.location(currentLocation);

			spawnUsingSettings(packagedValues);
		}
	}
}
