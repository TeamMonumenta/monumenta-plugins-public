package com.playmonumenta.plugins.particle;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Spawns particles evenly spread along a line.
 */
public class PPLine extends AbstractPartialParticle<PPLine> {

	private Vector mDirection;
	private double mLength;

	private double mParticlesPerMeter = -1;
	private double mMinParticlesPerMeter = 1;
	private double mGroupingDistance = 0.15;
	private int mAnimationTicks = 1;

	private boolean mIncludeStart = true;
	private boolean mIncludeEnd = true;
	private double mOffset = 0;

	public PPLine(Particle particle, Location startLocation, Location endLocation) {
		super(particle, startLocation);
		mDirection = LocationUtils.getDirectionTo(endLocation, startLocation);
		mLength = startLocation.distance(endLocation);
	}

	public PPLine(Particle particle, Location startLocation, Location endLocation, double delta) {
		super(particle, startLocation);
		mDirection = LocationUtils.getDirectionTo(endLocation, startLocation);
		mLength = startLocation.distance(endLocation);
		mDeltaX = delta;
		mDeltaY = delta;
		mDeltaZ = delta;
	}


	public PPLine(Particle particle, Location startLocation, Vector direction, double length) {
		super(particle, startLocation);
		mDirection = direction;
		mLength = length;
	}

	@Override
	public PPLine copy() {
		return copy(new PPLine(mParticle, mLocation.clone(), mDirection.clone(), mLength));
	}

	@Override
	public PPLine copy(PPLine copy) {
		super.copy(copy);
		copy.mDirection = mDirection.clone();
		copy.mLength = mLength;
		copy.mParticlesPerMeter = mParticlesPerMeter;
		copy.mMinParticlesPerMeter = mMinParticlesPerMeter;
		copy.mGroupingDistance = mGroupingDistance;
		copy.mAnimationTicks = mAnimationTicks;
		copy.mIncludeStart = mIncludeStart;
		copy.mIncludeEnd = mIncludeEnd;
		copy.mOffset = mOffset;
		return copy;
	}

	public PPLine location(Location start, Location end) {
		mLocation = start;
		mDirection = LocationUtils.getDirectionTo(end, start);
		mLength = start.distance(end);
		return this;
	}

	/**
	 * Shifts the start location by the given amount in the direction of this particle line.
	 * This shortens the line, but does not change the total number of particles spawned.
	 */
	public PPLine shiftStart(double shift) {
		mLocation.add(mDirection.clone().multiply(shift));
		mLength = Math.max(0, mLength - shift);
		return this;
	}

	/**
	 * Shifts the entire line by the given amount in the direction of this particle line.
	 */
	public PPLine shift(double shift) {
		mLocation.add(mDirection.clone().multiply(shift));
		return this;
	}

	public PPLine length(double length) {
		mLength = length;
		return this;
	}

	public PPLine scaleLength(double scale) {
		mLength *= scale;
		return this;
	}

	public PPLine delay(int animationTicks) {
		if (animationTicks < 1) {
			animationTicks = 1;
		}
		mAnimationTicks = animationTicks;
		return this;
	}

	/**
	 * Offsets the spawned particles by a factor of the distance between particles.
	 * Useful when drawing the same line multiple times over time, and wanting different particle locations each time.
	 * Disables drawing the end particle to not overshoot.
	 */
	public PPLine offset(double factor) {
		mOffset = factor;
		if (factor != 0) {
			mIncludeEnd = false;
		}
		return this;
	}

	/**
	 * Sets the number of particles per meter of length of this line.
	 * Also sets the minimum number per meter to 1/4th of this value.
	 */
	public PPLine countPerMeter(double countPerMeter) {
		mParticlesPerMeter = countPerMeter;
		mMinParticlesPerMeter = countPerMeter / 4;
		return this;
	}

	public PPLine minParticlesPerMeter(double minParticlesPerMeter) {
		mMinParticlesPerMeter = minParticlesPerMeter;
		return this;
	}

	public PPLine includeStart(boolean includeStart) {
		mIncludeStart = includeStart;
		return this;
	}

	public PPLine includeEnd(boolean includeEnd) {
		mIncludeEnd = includeEnd;
		return this;
	}

	/**
	 * Group particles closer than this distance together into a single spawn call. Useful to send fewer packets and thus cause less stress on the network.
	 */
	public PPLine groupingDistance(double groupingDistance) {
		mGroupingDistance = groupingDistance;
		return this;
	}

	@Override
	protected int getPartialCount(double multiplier, Player player, ParticleCategory source) {
		if (mParticlesPerMeter < 0) {
			return super.getPartialCount(multiplier, player, source);
		}
		int count = Math.max(mMinimumCount, (int) Math.ceil(mLength * Math.max(mParticlesPerMeter * multiplier, mMinParticlesPerMeter)));
		if (mIncludeStart && mIncludeEnd) {
			count++;
		} else if (!mIncludeStart && !mIncludeEnd && count > 1) {
			count--;
		}
		return count;
	}

	@Override
	protected void doSpawn(PartialParticleBuilder packagedValues) {
		Location loc = mLocation.clone();
		int count = packagedValues.count();
		packagedValues.count(1);
		int rawCount = count - (mIncludeStart && mIncludeEnd ? 1 : 0) + (!mIncludeStart && !mIncludeEnd ? 1 : 0);
		double stepLength = mLength / Math.max(1, rawCount);
		if (stepLength < mGroupingDistance && !mDirectionalMode && count > 2) {
			int grouping = Math.min(count, (int) Math.ceil(mGroupingDistance / stepLength));
			packagedValues.count(grouping);
			count /= grouping;
			stepLength = count <= 1 ? 0 : mLength / (count - 1);
		}
		Vector step = mDirection.clone().multiply(stepLength);
		if (mOffset != 0) {
			loc.add(step.clone().multiply(mOffset));
		}
		if (!mIncludeStart) {
			loc.add(step);
		}

		if (mAnimationTicks > 1) {
			final int finalCount = count;
			final int mCountPerTick = (int) Math.ceil((float) finalCount / mAnimationTicks);

			// run it once first
			packagedValues.location(loc);
			spawnUsingSettings(packagedValues);
			loc.add(step);

			new BukkitRunnable() {
				int mT = 1;
				int mI = 1;
				@Override
				public void run() {
					for (int j = 0; j <= mCountPerTick; j++) {
						packagedValues.location(loc);
						spawnUsingSettings(packagedValues);
						loc.add(step);
						mI++;

						if (mI > finalCount) {
							break;
						}
					}

					mT++;
					if (mT >= mAnimationTicks || mI > finalCount) {
						this.cancel();
					}
				}
			}.runTaskTimerAsynchronously(Plugin.getInstance(), 0, 1);
		} else {
			for (int i = 0; i < count; i++) {
				packagedValues.location(loc);
				spawnUsingSettings(packagedValues);
				loc.add(step);
			}
		}
	}
}
