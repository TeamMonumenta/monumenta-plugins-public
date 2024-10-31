package com.playmonumenta.plugins.particle;

import com.destroystokyo.paper.ParticleBuilder;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

// Custom builder to override the spawn method to use ParticleManager.sendParticle
public class PartialParticleBuilder extends ParticleBuilder {

	private @Nullable Entity mSourceEntity;

	public PartialParticleBuilder(Particle particle) {
		super(particle);
	}

	public PartialParticleBuilder copy() {
		PartialParticleBuilder copy = new PartialParticleBuilder(this.particle());
		copy.count(this.count());
		copy.extra(this.extra());
		copy.data(this.data());
		copy.force(this.force());
		copy.location(this.location());
		copy.offset(this.offsetX(), this.offsetY(), this.offsetZ());
		copy.receivers(this.receivers());
		copy.sourceEntity(this.sourceEntity());
		return copy;
	}

	@Override
	public ParticleBuilder source(@Nullable Player source) {
		this.mSourceEntity = source;
		return this;
	}

	@Override
	public @Nullable Player source() {
		if (mSourceEntity instanceof Player player) {
			return player;
		}
		return null;
	}

	public @Nullable Entity sourceEntity() {
		return mSourceEntity;
	}

	public PartialParticleBuilder sourceEntity(@Nullable Entity source) {
		this.mSourceEntity = source;
		return this;
	}

	/**
	 * Sends the particle to all receiving players (or all). This method is safe to use Asynchronously
	 *
	 * @return a reference to this object.
	 */
	@Override
	public ParticleBuilder spawn() {
		final Location location = this.location();
		if (location == null) {
			throw new IllegalStateException("Please specify location for this particle");
		}
		final List<Player> players = this.receivers() != null ? this.receivers() : location.getWorld().getPlayers();
		@Nullable
		final Entity source = this.sourceEntity();
		for (Player player : players) {
			if (player == null || !player.isOnline() || player.getWorld() != location.getWorld()) {
				// this.receivers().remove(player);
				continue;
			}
			// mimic canSee logic
			if (source != null && !player.canSee(source)) {
				// this.receivers().remove(player);
				continue;
			}
			// TODO: add a setting for the player's particle view distance
			/*
			if (location.distanceSquared(player.getLocation()) > AbstractPartialParticle.PARTICLE_SPAWN_DISTANCE_SQUARED) {
				continue;
			}
			*/
			ParticleManager.addParticleToQueue(this.particle(), player, location.getX(), location.getY(), location.getZ(),
					this.count(), this.offsetX(), this.offsetY(), this.offsetZ(), this.extra(), this.data(), this.force());
		}
		return this;
	}
}
