package com.playmonumenta.plugins.player;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;



public class PartialParticle {
	@NotNull public Particle mParticle;
	@NotNull public Location mLocation;
	public int mCount;
	public double mOffsetX;
	public double mOffsetY;
	public double mOffsetZ;
	public double mExtra;
	@Nullable public Object mData;

	// Share same offset, skip extra
	public PartialParticle(@NotNull Particle particle, @NotNull Location location, int count, double offset, double extra) {
		this(particle, location, count, offset, offset, offset, extra);
	}

	// Share same offset, skip data
	public PartialParticle(@NotNull Particle particle, @NotNull Location location, int count, double offset, @Nullable Object data) {
		this(particle, location, count, offset, offset, offset, data);
	}

	// Share same offset
	public PartialParticle(@NotNull Particle particle, @NotNull Location location, int count, double offset, double extra, @Nullable Object data) {
		this(particle, location, count, offset, offset, offset, extra, data);
	}

	// Skip extra
	public PartialParticle(@NotNull Particle particle, @NotNull Location location, int count, double offsetX, double offsetY, double offsetZ, @Nullable Object data) {
		// Default extra is 1 - https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/browse/src/main/java/org/bukkit/craftbukkit/CraftWorld.java#2284
		this(particle, location, count, offsetX, offsetY, offsetZ, 1, data);
	}

	// Skip data
	public PartialParticle(@NotNull Particle particle, @NotNull Location location, int count, double offsetX, double offsetY, double offsetZ, double extra) {
		this(particle, location, count, offsetX, offsetY, offsetZ, extra, null);
	}

	// Player width 0.6, offset goes both positive & negative direction
	public PartialParticle(
		@NotNull Particle particle,
		@NotNull Location location,
		int count,
		double offsetX,
		double offsetY,
		double offsetZ,
		double extra,
		@Nullable Object data
	) {
		mParticle = particle;
		mLocation = location;
		mCount = count;
		mOffsetX = offsetX;
		mOffsetY = offsetY;
		mOffsetZ = offsetZ;
		mExtra = extra;
		mData = data;
	}

	// Spawns this particle, randomly offset for each player near its location,
	// based on each player's individual particle multiplier settings
	public @NotNull PartialParticle spawn() {
		return spawnExcept(null);
	}

	// Spawns this particle for the specified player, based on their particle multiplier setting
	public @NotNull PartialParticle spawn(@NotNull Player player) {
		return spawnWithCount(
			player,
			getPartialCount(player)
		);
	}

	// Spawns this particle for the specified player, but with the specified overwritten particle count
	public @NotNull PartialParticle spawnWithCount(@NotNull Player player, int count) {
		player.spawnParticle(
			mParticle,
			mLocation,
			count,
			mOffsetX,
			mOffsetY,
			mOffsetZ,
			mExtra,
			mData
		);
		return this;
	}

	// Like spawn(), but excludes the specified source player if they turned off self particles.
	// Most useful for things like cosmetic enchants
	public @NotNull PartialParticle spawnHideable(@NotNull Player sourcePlayer) {
		if (new PlayerData(sourcePlayer).checkSelfParticles()) {
			return spawn();
		} else {
			return spawnExcept(sourcePlayer);
		}
	}

	// Like spawn(), but excludes the specified source player
	public @NotNull PartialParticle spawnExcept(@Nullable Player sourcePlayer) {
		// Grab all players regardless of custom "non-targetable" status so they can see particles too.
		// Use cheaper cube bounding box check instead of PlayerUtils' sphere via distance comparison
		for (Player player : mLocation.getNearbyPlayers(30)) {
			if (sourcePlayer != null && sourcePlayer == player) {
				continue;
			}

			spawn(player);
		}
		return this;
	}

	public int getPartialCount(@NotNull Player player) {
		// Ceil so that when count > 0, players will always see at least something
		return (int)Math.ceil(
			mCount * new PlayerData(player).checkParticleMultiplier()
		);
	}
}