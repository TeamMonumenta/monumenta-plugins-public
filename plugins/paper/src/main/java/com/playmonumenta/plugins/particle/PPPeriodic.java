package com.playmonumenta.plugins.particle;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

/**
 * Same as {@link PartialParticle}, except that when the particles count is lowered below 1, it will periodically spawn a particle instead of randomly.
 * For this to work this PartialParticle must be saves (as a field) and re-used!
 */
public class PPPeriodic extends AbstractPartialParticle<PPPeriodic> {

	private int mPeriod = 0;

	public PPPeriodic(Particle particle, Location location) {
		super(particle, location);
		mMinimumMultiplier = true;
	}

	// makes no sense for this particle type
	@Deprecated
	@Override
	public PPPeriodic minimumMultiplier(boolean minimumMultiplier) {
		return this;
	}

	@Override
	protected void prepareSpawn() {
		mPeriod++;
	}

	@Override
	protected int getPartialCount(double multipliedCount, Player player, ParticleCategory source) {
		if (multipliedCount < 1) {
			return (int) Math.floor(multipliedCount * mPeriod) - (int) Math.floor(multipliedCount * (mPeriod - 1));
		}
		return super.getPartialCount(multipliedCount, player, source);
	}

}
