package com.playmonumenta.plugins.particle;

import com.destroystokyo.paper.ParticleBuilder;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;

/**
 * Particles at multiple locations. If the count of particles per locations gets lower than one, it will regularly space out particles.
 */
public class PPMulti extends AbstractPartialParticle<PPMulti> {

	private final List<Location> mLocations;

	public PPMulti(Particle particle, List<Location> locations) {
		super(particle, locations.get(0));
		mLocations = locations;
	}

	public PPMulti countPerLocation(int count) {
		return super.count(count * mLocations.size());
	}

	@Override
	protected void doSpawn(ParticleBuilder packagedValues) {
		double countPerLocation = 1.0 * packagedValues.count() / mLocations.size();
		if (countPerLocation >= 1) { // at least one per location: spawn the same amount each time
			packagedValues.count((int) countPerLocation);
			for (Location l : mLocations) {
				packagedValues.location(l);
				spawnUsingSettings(packagedValues);
			}
		} else { // less than one per location: spawn single particles while skipping locations
			packagedValues.count(1);
			double currentCount = 1; // spawn one immediately
			for (Location l : mLocations) {
				if (currentCount >= 1) {
					packagedValues.location(l);
					spawnUsingSettings(packagedValues);
					currentCount--;
				}
				currentCount += countPerLocation;
			}
		}
	}

}
