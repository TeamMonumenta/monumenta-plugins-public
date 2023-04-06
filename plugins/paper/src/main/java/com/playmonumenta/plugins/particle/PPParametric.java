package com.playmonumenta.plugins.particle;

import com.destroystokyo.paper.ParticleBuilder;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

/**
 * A {@link PartialParticle} that takes a function with a single parameter (ranging from 0 to 1) to spawn multiple particles in some parametric way.
 * The function can for example parameterize the position of the particles to form arbitrary curves, while also altering the delta or other values along the curve.
 *
 * @see PPCircle PPCircle to draw circles and rings
 * @see PPLine PPLine to draw simple lines
 */
public class PPParametric extends AbstractPartialParticle<PPParametric> {

	public interface ParametricFunction {
		void transform(double parameter, ParticleBuilder packagedValues);
	}

	private final ParametricFunction mFunction;

	private boolean mIncludeStart = true;
	private boolean mIncludeEnd = false;

	public PPParametric(Particle particle, Location location, ParametricFunction function) {
		super(particle, location);
		mFunction = function;
	}

	public PPParametric includeStart(boolean includeStart) {
		mIncludeStart = includeStart;
		return this;
	}

	public PPParametric includeEnd(boolean includeEnd) {
		mIncludeEnd = includeEnd;
		return this;
	}

	@Override
	protected int getPartialCount(double multiplier, Player player, ParticleCategory source) {
		int count = super.getPartialCount(multiplier, player, source);
		if (mIncludeStart && mIncludeEnd) {
			count++;
		} else if (!mIncludeStart && !mIncludeEnd && count > 1) {
			count--;
		}
		return count;
	}

	@Override
	protected void doSpawn(ParticleBuilder packagedValues) {
		int count = packagedValues.count();
		packagedValues.count(1);
		for (int i = 0; i <= count; i++) {
			if ((i == 0 && !mIncludeStart) || (i == count && !mIncludeEnd)) {
				continue;
			}
			mFunction.transform(1.0 * i / count, packagedValues);
			spawnUsingSettings(packagedValues);
		}
	}

}
