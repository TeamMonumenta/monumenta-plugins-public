package com.playmonumenta.plugins.particle;

import java.util.Collection;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class PPBatch extends AbstractPartialParticle<PPBatch> {
	public interface BatchFunction {
		// Return true to spawn the particle, false to skip it
		boolean transform(int index, AbstractPartialParticle<?> particle);
	}

	private static final Location PLACEHOLDER_LOCATION = new Location(null, 0, 0, 0);
	private static final Particle PLACEHOLDER_PARTICLE = Particle.COMPOSTER;
	protected final Queue<AbstractPartialParticle<?>> mPendingParticles = new LinkedBlockingQueue<>();
	protected final @Nullable BatchFunction mFunction;

	public PPBatch() {
		super(PLACEHOLDER_PARTICLE, PLACEHOLDER_LOCATION);
		mFunction = null;
	}

	// Specifying a location is faster, but optional in case you want to use forEachNearbyPlayer on individual particles instead
	public PPBatch(Location location) {
		super(PLACEHOLDER_PARTICLE, location);
		mFunction = null;
	}

	public PPBatch(BatchFunction function) {
		super(PLACEHOLDER_PARTICLE, PLACEHOLDER_LOCATION);
		mFunction = function;
	}

	public PPBatch(Location location, BatchFunction function) {
		super(PLACEHOLDER_PARTICLE, location);
		mFunction = function;
	}

	public PPBatch add(AbstractPartialParticle<?> particle) {
		mPendingParticles.add(particle);
		return getSelf();
	}


	@Override
	public PPBatch spawnForPlayers(ParticleCategory source, ParticleCategory otherSource, @Nullable Collection<Player> players, @Nullable Player sourcePlayer) {
		final Collection<Player> finalPlayers = players != null && !players.isEmpty() ? players : forEachNearbyPlayer();
		ParticleManager.runOffMainThread(() -> {
			int i = 0;
			for (AbstractPartialParticle<?> particle : mPendingParticles) {
				if (mFunction == null || mFunction.transform(i, particle)) {
					particle.spawnForPlayers(source, otherSource, finalPlayers, sourcePlayer);
				}
				i++;
			}
		});
		return getSelf();
	}

	// Prevent NPE if no location was specified
	@Override
	public Collection<Player> forEachNearbyPlayer() {
		if (mLocation != null && mLocation.getWorld() != null) {
			return super.forEachNearbyPlayer();
		}
		return Collections.emptyList();
	}

	@Override
	public void doSpawn(PartialParticleBuilder packagedValues) {
		// This should never be called
	}
}
