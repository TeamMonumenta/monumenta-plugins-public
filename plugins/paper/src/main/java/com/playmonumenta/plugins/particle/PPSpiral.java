package com.playmonumenta.plugins.particle;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class PPSpiral extends AbstractPartialParticle<PPSpiral> {
	/*
	 * Radius of the circle within which all particles must
	 * start, prior to applying deltas (which may put them outside).
	 */
	protected double mRadius;

	/*
	 * Angle of each curve over the entire spiral.
	 * eg. a value of 180 -> a curve which stated
	 * infront of the center ends up behind the center,
	 */
	protected double mCurveAngle = 270;
	protected int mTicks = Constants.TICKS_PER_SECOND;
	protected int mCurves = 3;

	protected int mCountPerBlockPerCurve = -1;

	/*-------------------------------------------------------------------------------
	 * Constructors
	 */
	public PPSpiral(Particle particle, Location centerLocation, double radius) {
		super(particle, centerLocation);
		mRadius = radius;
		/*
		 * Here just to make sure PPSpiral instantiations without .count()
		 * or .countPerBlockPerCurve() arent blank.
		 */
		mCount = (int) (radius * mCurves * 10);
	}

	@Override
	public PPSpiral copy() {
		return copy(new PPSpiral(mParticle, mLocation.clone(), mRadius));
	}

	@Override
	public PPSpiral copy(PPSpiral copy) {
		super.copy(copy);
		copy.mRadius = mRadius;
		copy.mCurveAngle = mCurveAngle;
		copy.mTicks = mTicks;
		copy.mCurves = mCurves;
		copy.mCountPerBlockPerCurve = mCountPerBlockPerCurve;
		return copy;
	}

	/*-------------------------------------------------------------------------------
	 * Parameter getters and setters
	 */
	public PPSpiral curveAngle(double curveAngle) {
		mCurveAngle = curveAngle;
		return this;
	}

	public double curveAngle() {
		return mCurveAngle;
	}

	public PPSpiral countPerBlockPerCurve(int countPerBlockPerCurve) {
		mCountPerBlockPerCurve = countPerBlockPerCurve;
		return this;
	}

	public int countPerBlockPerCurve() {
		return mCountPerBlockPerCurve;
	}

	@Override
	public PPSpiral count(int count) {
		super.count(count);
		mCountPerBlockPerCurve = -1;
		return this;
	}

	public PPSpiral curves(int curves) {
		mCurves = curves;
		return this;
	}

	public int curves() {
		return mCurves;
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
	protected int getPartialCount(double multiplier, Player player, ParticleCategory source) {
		if (mCountPerBlockPerCurve != -1) {
			mCount = (int) (mCountPerBlockPerCurve * multiplier * mRadius * mCurves);
		}
		return super.getPartialCount(multiplier, player, source);
	}

	// 1 degree per 0.025 blocks (radial outwards!)
	// 40 degrees per block
	@Override
	protected void doSpawn(PartialParticleBuilder packagedValues) {
		Location centerLocation = packagedValues.location();
		// Spawning one by one, looping manually by partialCount times.
		// spawnWithSettings() will handle whether count should be 0 for
		// directional mode
		int partialCount = packagedValues.count();
		packagedValues.count(1);
		if (centerLocation == null) {
			return;
		}

		new BukkitRunnable() {
			final double mRadiusIncrementPerTick = mRadius / mTicks;
			final double mParticlesPerCurvePerTick = (double) partialCount / mCurves / mTicks;
			final double mDegPerCurve = 360.0 / mCurves;
			final double mDegreeOffset = mCurveAngle * mCurves / partialCount;
			double mCurrentRadius = 0;
			double mCurrentDegree = 0;
			int mSafety = 0;

			@Override
			public void run() {
				try {
					mSafety++;
					if (mSafety > 300) {
						this.cancel();
						return;
					}
					Location loc = centerLocation.clone();
					for (int s = 0; s < mCurves; s++) {
						if (mParticlesPerCurvePerTick >= 1) {
							for (int i = 0; i < mParticlesPerCurvePerTick; i++) {
								final double mRadiusOffset = i * mRadiusIncrementPerTick / mParticlesPerCurvePerTick + mCurrentRadius;
								double x = FastUtils.cos((mCurrentDegree + i * mDegreeOffset + (s * mDegPerCurve)) * (Math.PI / 180)) * mRadiusOffset;
								double z = FastUtils.sin((mCurrentDegree + i * mDegreeOffset + (s * mDegPerCurve)) * (Math.PI / 180)) * mRadiusOffset;
								loc.add(x, 0, z);
								packagedValues.location(loc);

								spawnUsingSettings(packagedValues);
								loc.subtract(x, 0, z);
								if (mRadiusOffset > mRadius) {
									break;
								}
							}
						} else {
							if (FastUtils.RANDOM.nextDouble() < mParticlesPerCurvePerTick) {
								final double mRadiusOffset = mRadiusIncrementPerTick + mCurrentRadius;
								double x = FastUtils.cos((mCurrentDegree + mDegreeOffset + (s * mDegPerCurve)) * (Math.PI / 180)) * mRadiusOffset;
								double z = FastUtils.sin((mCurrentDegree + mDegreeOffset + (s * mDegPerCurve)) * (Math.PI / 180)) * mRadiusOffset;
								loc.add(x, 0, z);
								packagedValues.location(loc);

								spawnUsingSettings(packagedValues);
								loc.subtract(x, 0, z);
							}
						}
					}
					mCurrentDegree += (mParticlesPerCurvePerTick * mDegreeOffset);
					mCurrentRadius += mRadiusIncrementPerTick;
					if (mCurrentRadius > mRadius) {
						this.cancel();
					}
				} catch (Exception e) {
					this.cancel();
					throw e;
				}
			}
		}.runTaskTimerAsynchronously(Plugin.getInstance(), 0, 1);
	}
}
