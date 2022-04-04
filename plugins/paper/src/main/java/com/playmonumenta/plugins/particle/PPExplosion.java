package com.playmonumenta.plugins.particle;

import com.destroystokyo.paper.ParticleBuilder;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Location;
import org.bukkit.Particle;

/**
 * Spawns particles at the center, moving outwards.
 * Delta is used to vary spawn locations of particles around the center (non-gaussian).
 */
public class PPExplosion extends AbstractPartialParticle<PPExplosion> {

	private boolean mFlat = false;

	private double mSpeed = 1;
	private double mSpeedVar = 0;

	public PPExplosion(Particle particle, Location center) {
		super(particle, center);
		mDirectionalMode = true;
	}

	/**
	 * Whether this explosion is flat (circular) or spherical
	 */
	public PPExplosion flat(boolean flat) {
		mFlat = flat;
		return this;
	}

	/**
	 * Speed of the particles
	 */
	public PPExplosion speed(double speed) {
		mSpeed = speed;
		return this;
	}

	/**
	 * variance in particle speed
	 */
	public PPExplosion speedVar(double speedVar) {
		mSpeedVar = speedVar;
		return this;
	}

	@Override
	protected void doSpawn(ParticleBuilder packagedValues) {
		mDirectionalMode = true;
		int count = packagedValues.count();
		Location loc = packagedValues.location();
		if (loc == null) {
			return;
		}
		packagedValues.count(1);
		for (int i = 0; i < count; i++) {
			if (mDeltaX != 0 || mDeltaY != 0 || mDeltaZ != 0) {
				packagedValues.location(loc.getWorld(),
					loc.getX() + FastUtils.randomDoubleInRange(-mDeltaX, mDeltaX),
					loc.getY() + FastUtils.randomDoubleInRange(-mDeltaY, mDeltaY),
					loc.getZ() + FastUtils.randomDoubleInRange(-mDeltaZ, mDeltaZ));
			}
			double speed = mSpeed + FastUtils.randomDoubleInRange(-mSpeedVar, mSpeedVar);
			double phi = FastUtils.randomDoubleInRange(0, Math.PI * 2);
			if (mFlat) {
				packagedValues.offset(speed * FastUtils.cos(phi), 0, speed * FastUtils.sin(phi));
			} else {
				double theta = FastUtils.randomDoubleInRange(0, Math.PI);
				double sinTheta = FastUtils.sin(theta);
				packagedValues.offset(speed * FastUtils.cos(phi) * sinTheta, speed * FastUtils.cos(theta), speed * FastUtils.sin(phi) * sinTheta);
			}
			spawnUsingSettings(packagedValues);
		}
	}

}
