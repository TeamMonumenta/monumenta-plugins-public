package com.playmonumenta.plugins.particle;

import com.destroystokyo.paper.ParticleBuilder;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Location;
import org.bukkit.Particle;


public class PPPillar extends AbstractPartialParticle<PPPillar> {
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

	public PPPillar(Particle particle, Location baseLocation, double height) {
		super(particle, baseLocation);
		mHeight = height;
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
