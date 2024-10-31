package com.playmonumenta.plugins.particle;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;

public class PPSpiral extends AbstractPartialParticle<PPSpiral> {
	/*
	 * Radius of the circle within which all particles must
	 * start, prior to applying deltas (which may put them outside).
	 */
	protected double mRadius;

	protected double mDistancePerParticle = .025;
	protected int mTicks = Constants.TICKS_PER_SECOND;

	/*-------------------------------------------------------------------------------
	 * Constructors
	 */

	public PPSpiral(Particle particle, Location centerLocation, double radius) {
		super(particle, centerLocation);
		mRadius = radius;
	}

	@Override
	public PPSpiral copy() {
		return copy(new PPSpiral(mParticle, mLocation.clone(), mRadius));
	}

	@Override
	public PPSpiral copy(PPSpiral copy) {
		super.copy(copy);
		copy.mRadius = mRadius;
		copy.mDistancePerParticle = mDistancePerParticle;
		copy.mTicks = mTicks;
		return copy;
	}

	/*-------------------------------------------------------------------------------
	 * Parameter getters and setters
	 */

	public PPSpiral distancePerParticle(double distancePerParticle) {
		mDistancePerParticle = distancePerParticle;
		return this;
	}

	public double distancePerParticle() {
		return mDistancePerParticle;
	}

	public PPSpiral radius(double radius) {
		mRadius = radius;
		return this;
	}

	public double radius() {
		return mRadius;
	}

	public PPSpiral ticks(int ticks) {
		mTicks = ticks;
		return this;
	}

	public int ticks() {
		return mTicks;
	}

	/*-------------------------------------------------------------------------------
	 * Methods
	 */

	@Override
	protected void doSpawn(PartialParticleBuilder packagedValues) {
		Location centerLocation = packagedValues.location();
		// Spawning one by one, looping manually by partialCount times.
		// spawnWithSettings() will handle whether count should be 0 for
		// directional mode
		packagedValues.count(1);
		if (centerLocation == null) {
			return;
		}

		new BukkitRunnable() {
		double mCurrentRadius = 0;
		int mCurrentDegree = 0;
		int mSafety = 0;
		final int mTotalParticles = (int) Math.floor(mRadius / mDistancePerParticle);
		final int mParticlesPerTick = mTotalParticles / mTicks;

			@Override
			public void run() {
				try {
					mSafety++;
					if (mCurrentRadius >= mRadius || mSafety > 300) {
						this.cancel();
						return;
					}
					Location loc = centerLocation.clone();
					for (double i = mCurrentRadius; i < mCurrentRadius + (mParticlesPerTick * mDistancePerParticle); i += mDistancePerParticle) {
						for (int j = 0; j < 3; j++) {
							double x = FastUtils.cos((mCurrentDegree + (j * 120)) * (Math.PI / 180)) * i;
							double z = FastUtils.sin((mCurrentDegree++ + (j * 120)) * (Math.PI / 180)) * i;
							loc.add(x, 0, z);
							packagedValues.location(loc);

							spawnUsingSettings(packagedValues);
							loc.subtract(x, 0, z);
						}
					}
					mCurrentRadius += mParticlesPerTick * mDistancePerParticle;
				} catch (Exception e) {
					this.cancel();
					throw e;
				}
			}
		}.runTaskTimerAsynchronously(Plugin.getInstance(), 0, 1);
	}
}
