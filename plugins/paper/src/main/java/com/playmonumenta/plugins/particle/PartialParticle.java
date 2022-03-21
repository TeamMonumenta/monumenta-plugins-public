package com.playmonumenta.plugins.particle;

import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Particle;


public class PartialParticle extends AbstractPartialParticle<PartialParticle> {

	/*
	 * Minimal constructor, useful for builder pattern
	 */
	public PartialParticle(Particle particle, Location location) {
		super(particle, location);
	}

	public PartialParticle(Particle particle, Location location, int count) {
		super(particle, location);
		mCount = count;
	}

	/*
	 * Share the same delta for X, Y and Z.
	 * Use default data.
	 * Use default directional/variance settings.
	 * Use default multiplier mode.
	 */
	public PartialParticle(Particle particle, Location location, int count, double delta, double extra) {
		this(particle, location, count, delta, delta, delta, extra);
	}

	/*
	 * Share the same delta for X, Y and Z.
	 * Use default multiplier mode.
	 */
	public PartialParticle(Particle particle, Location location, int count, double delta, double extra, @Nullable Object data, boolean directionalMode, double extraVariance) {
		this(particle, location, count, delta, delta, delta, extra, data, directionalMode, extraVariance);
	}

	/*
	 * Share the same delta for X, Y and Z.
	 */
	public PartialParticle(Particle particle, Location location, int count, double delta, double extra, @Nullable Object data, boolean directionalMode, double extraVariance, boolean minimumMultiplier) {
		this(particle, location, count, delta, delta, delta, extra, data, directionalMode, extraVariance, minimumMultiplier);
	}

	/*
	 * Use default data.
	 * Use default directional/variance settings.
	 * Use default multiplier mode.
	 */
	public PartialParticle(Particle particle, Location location, int count, double deltaX, double deltaY, double deltaZ) {
		this(particle, location, count, deltaX, deltaY, deltaZ, 0);
	}

	/*
	 * Use default data.
	 * Use default directional/variance settings.
	 * Use default multiplier mode.
	 */
	public PartialParticle(Particle particle, Location location, int count, double deltaX, double deltaY, double deltaZ, double extra) {
		this(particle, location, count, deltaX, deltaY, deltaZ, extra, null);
	}

	/*
	 * Use default directional/variance settings.
	 * Use default multiplier mode.
	 */
	public PartialParticle(Particle particle, Location location, int count, double deltaX, double deltaY, double deltaZ, @Nullable Object data) {
		this(particle, location, count, deltaX, deltaY, deltaZ, 0, data, false, 0);
	}

	/*
	 * Use default directional/variance settings.
	 * Use default multiplier mode.
	 */
	public PartialParticle(Particle particle, Location location, int count, double deltaX, double deltaY, double deltaZ, double extra, @Nullable Object data) {
		this(particle, location, count, deltaX, deltaY, deltaZ, extra, data, false, 0);
	}

	/*
	 * Use default multiplier mode.
	 */
	public PartialParticle(Particle particle, Location location, int count, double deltaX, double deltaY, double deltaZ, double extra, @Nullable Object data, boolean directionalMode, double extraVariance) {
		this(particle, location, count, deltaX, deltaY, deltaZ, extra, data, directionalMode, extraVariance, true);
	}

	public PartialParticle(
		Particle particle,
		Location location,
		int count,
		double deltaX,
		double deltaY,
		double deltaZ,
		double extra,
		@Nullable Object data,
		boolean directionalMode,
		double extraVariance,
		boolean minimumMultiplier
	) {
		super(particle, location);
		mCount = count;
		mDeltaX = deltaX;
		mDeltaY = deltaY;
		mDeltaZ = deltaZ;
		mExtra = extra;
		mData = data;
		mDirectionalMode = directionalMode;
		mExtraVariance = extraVariance;
		mMinimumMultiplier = minimumMultiplier;
	}

}
