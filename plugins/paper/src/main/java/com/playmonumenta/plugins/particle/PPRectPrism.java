package com.playmonumenta.plugins.particle;

import com.playmonumenta.plugins.utils.ParticleUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

/**
 * Spawns particles evenly spread along a cube.
 */
public class PPRectPrism extends AbstractPartialParticle<PPRectPrism> {
	private final Location mStartLocation;
	private final Vector mSize;

	private boolean mEdgeMode = false;
	private double mParticlesPerMeter = 1;
	private @Nullable Color mStartColor = null;
	private @Nullable Color mEndColor = null;
	private float mDustSize = 1f;

	/*-------------------------------------------------------------------------------
	 * Constructors
	 */

	public PPRectPrism(Particle particle, Location startCorner, Location endCorner) {
		super(particle, startCorner);
		mStartLocation = startCorner;
		mSize = endCorner.toVector().subtract(mStartLocation.toVector());
	}

	@Override
	public PPRectPrism copy() {
		return copy(new PPRectPrism(mParticle, mStartLocation.clone(), mStartLocation.clone().add(mSize)));
	}

	@Override
	public PPRectPrism copy(PPRectPrism copy) {
		super.copy(copy);
		copy.mEdgeMode = mEdgeMode;
		copy.mParticlesPerMeter = mParticlesPerMeter;
		copy.mStartColor = mStartColor;
		copy.mEndColor = mEndColor;
		copy.mDustSize = mDustSize;
		return copy;
	}

	/*-------------------------------------------------------------------------------
	 * Parameter getters and setters
	 */

	public PPRectPrism edgeMode(boolean edgeMode) {
		mEdgeMode = edgeMode;
		return this;
	}

	public boolean edgeMode() {
		return mEdgeMode;
	}

	public PPRectPrism countPerMeter(double countPerMeter) {
		mParticlesPerMeter = countPerMeter;
		return this;
	}

	public double countPerMeter() {
		return mParticlesPerMeter;
	}

	public PPRectPrism countPerMeterSquared(double countPerMeterSquared) {
		mParticlesPerMeter = Math.sqrt(countPerMeterSquared);
		return this;
	}

	public double countPerMeterSquared() {
		return mParticlesPerMeter * mParticlesPerMeter;
	}

	public PPRectPrism gradientColor(Color startColor, Color endColor, float dustSize) {
		mStartColor = startColor;
		mEndColor = endColor;
		mDustSize = dustSize;
		return this;
	}

	/*-------------------------------------------------------------------------------
	 * Methods
	 */

	@Override
	protected void doSpawn(PartialParticleBuilder packagedValues) {
		super.doSpawn(packagedValues);

		double count = packagedValues.count();
		packagedValues.count(1);
		Location loc = mStartLocation.clone();

		double stepSize = 1 / (mParticlesPerMeter * count);

		if (mEdgeMode) {
			// A rectangular prism is composed of 12 edges
			// They can be divided into 3 groups of 4 parallel lines, along each axis
			for (double x = 0; x < mSize.getX(); x += stepSize) {
				offsetAndSpawn(packagedValues, loc, x, 0, 0);
				offsetAndSpawn(packagedValues, loc, x, 0, mSize.getZ());
				offsetAndSpawn(packagedValues, loc, x, mSize.getY(), 0);
				offsetAndSpawn(packagedValues, loc, x, mSize.getY(), mSize.getZ());
			}
			for (double y = 0; y < mSize.getY(); y += stepSize) {
				offsetAndSpawn(packagedValues, loc, 0, y, 0);
				offsetAndSpawn(packagedValues, loc, 0, y, mSize.getZ());
				offsetAndSpawn(packagedValues, loc, mSize.getX(), y, 0);
				offsetAndSpawn(packagedValues, loc, mSize.getX(), y, mSize.getZ());
			}
			for (double z = 0; z < mSize.getZ(); z += stepSize) {
				offsetAndSpawn(packagedValues, loc, 0, 0, z);
				offsetAndSpawn(packagedValues, loc, 0, mSize.getY(), z);
				offsetAndSpawn(packagedValues, loc, mSize.getX(), 0, z);
				offsetAndSpawn(packagedValues, loc, mSize.getX(), mSize.getY(), z);
			}
		} else {
			// A rectangular prism is composed of 6 faces
			// They can be divided into 3 groups of 2 parallel faces, along each axis
			for (double x = 0; x < mSize.getX(); x += stepSize) {
				for (double y = 0; y < mSize.getY(); y += stepSize) {
					offsetAndSpawn(packagedValues, loc, x, y, 0);
					offsetAndSpawn(packagedValues, loc, x, y, mSize.getZ());
				}
			}
			for (double y = 0; y < mSize.getY(); y += stepSize) {
				for (double z = 0; z < mSize.getZ(); z += stepSize) {
					offsetAndSpawn(packagedValues, loc, 0, y, z);
					offsetAndSpawn(packagedValues, loc, mSize.getX(), y, z);
				}
			}
			for (double x = 0; x < mSize.getX(); x += stepSize) {
				for (double z = 0; z < mSize.getZ(); z += stepSize) {
					offsetAndSpawn(packagedValues, loc, x, 0, z);
					offsetAndSpawn(packagedValues, loc, x, mSize.getY(), z);
				}
			}
		}
	}

	private void offsetAndSpawn(PartialParticleBuilder packagedValues, Location loc, double x, double y, double z) {
		if (mStartColor != null && mEndColor != null) {
			double distance = Math.sqrt(x * x + y * y + z * z) / mSize.length();
			packagedValues.data(new Particle.DustOptions(ParticleUtils.getTransition(mStartColor, mEndColor, distance), mDustSize));
		}
		packagedValues.location(loc.clone().add(x, y, z));
		spawnUsingSettings(packagedValues);
	}
}
