package com.playmonumenta.plugins.particle;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

/**
 * Same as {@link PartialParticle}, except that when the particles count is lowered below 1, it will periodically spawn a particle instead of randomly.
 * For this to work this PartialParticle must be saved (as a field) and re-used, or {@link #manualTimeOverride(int)} be called with a time value argument that increases by exactly one between calls.
 */
public class PPPeriodic extends AbstractPartialParticle<PPPeriodic> {

	private int mTime = 0;

	public PPPeriodic(Particle particle, Location location) {
		super(particle, location);
		mMinimumCount = 0;
	}

	public PPPeriodic manualTimeOverride(int time) {
		mTime = time;
		return getSelf();
	}

	@Override
	protected void prepareSpawn() {
		mTime++;
	}

	@Override
	protected int getPartialCount(double multiplier, Player player, ParticleCategory source) {
		double multipliedCount = mCount * multiplier;
		if (multipliedCount < 1) {
			return (int) Math.floor(multipliedCount * mTime) - (int) Math.floor(multipliedCount * (mTime - 1));
		}
		return (int) Math.ceil(multipliedCount);
	}

}
