package com.playmonumenta.plugins.particle;

import com.destroystokyo.paper.ParticleBuilder;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

/**
 * Spawns particles evenly spread along a line.
 */
public class PPLine extends AbstractPartialParticle<PPLine> {

	private final Vector mDirection;
	private double mLength;

	private double mMinParticlesPerMeter = 1;
	private double mGroupingDistance = 0.2;

	public PPLine(Particle particle, Location startLocation, Location endLocation) {
		super(particle, startLocation);
		mDirection = LocationUtils.getDirectionTo(endLocation, startLocation);
		mLength = startLocation.distance(endLocation);
	}

	public PPLine(Particle particle, Location startLocation, Vector direction, double length) {
		super(particle, startLocation);
		mDirection = direction;
		mLength = length;
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

	public PPLine countPerMeter(double countPerMeter) {
		return super.count((int) Math.ceil(countPerMeter * mLength));
	}

	public PPLine minParticlesPerMeter(double minParticlesPerMeter) {
		mMinParticlesPerMeter = minParticlesPerMeter;
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
	protected void doSpawn(ParticleBuilder packagedValues) {
		Location loc = mLocation.clone();
		int count = Math.max(packagedValues.count(), (int) Math.ceil(mLength * mMinParticlesPerMeter));
		packagedValues.count(1);
		double stepLength = mLength / count;
		if (stepLength < mGroupingDistance) {
			int grouping = Math.min(count, (int) Math.ceil(mGroupingDistance / stepLength));
			stepLength *= grouping;
			packagedValues.count(grouping);
			count /= grouping;
		}
		Vector step = mDirection.clone().multiply(stepLength);
		for (int i = 0; i < count; i++) {
			packagedValues.location(loc);
			spawnUsingSettings(packagedValues);
			loc.add(step);
		}
	}

}
