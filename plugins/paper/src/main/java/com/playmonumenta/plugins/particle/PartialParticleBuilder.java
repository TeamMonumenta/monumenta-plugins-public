package com.playmonumenta.plugins.particle;

import com.destroystokyo.paper.ParticleBuilder;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

// Custom builder to override the spawn method to use ParticleManager.sendParticle
public class PartialParticleBuilder extends ParticleBuilder {
	public PartialParticleBuilder(Particle particle) {
		super(particle);
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
		@Nullable final Player source = this.source();
		for (Player player : players) {
			if (player == null || !player.isOnline() || player.getWorld() != location.getWorld()) {
				this.receivers().remove(player);
				continue;
			}
			// mimic canSee logic
			if (source != null && !player.canSee(source)) {
				this.receivers().remove(player);
				continue;
			}
			// TODO: add a setting for the player's particle view distance
			/*
			if (location.distanceSquared(player.getLocation()) > AbstractPartialParticle.PARTICLE_SPAWN_DISTANCE_SQUARED) {
				continue;
			}
			*/
			ParticleManager.addParticleToQueue(this.particle(), player, location.getX(), location.getY(), location.getZ(), this.count(), this.offsetX(), this.offsetY(), this.offsetZ(), this.extra(), this.data(), this.force());
		}
		return this;
	}
}
