package com.playmonumenta.plugins.player;

import java.util.function.Consumer;

import com.playmonumenta.plugins.Constants.Objectives;
import com.playmonumenta.plugins.utils.FastUtils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;



public class PartialParticle {
	// https://minecraft.fandom.com/wiki/Commands/particle#Arguments
	// https://papermc.io/javadocs/paper/1.16/org/bukkit/entity/Player.html#spawnParticle-org.bukkit.Particle-org.bukkit.Location-int-double-double-double-double-T-
	public @NotNull Particle mParticle;
	public @NotNull Location mLocation;
	public int mCount;
	public double mDeltaX;
	public double mDeltaY;
	public double mDeltaZ;
	public double mExtra;
	public @Nullable Object mData;

	/*
	 * By default, this PartialParticle gets spawned normally,
	 * starting varied from mLocation based on the mDeltas,
	 * and drifting based on mExtra (particle must support setting a speed).
	 * Set to true to use the mDelta for each axis to randomise between -mDelta
	 * and +mDelta for directional movement instead,
	 * meaning the particles spawn at mLocation,
	 * individually moving in the direction of varied X, Y and Z values.
	 * Higher mDeltas or mExtras would cause faster and further movement.
	 *
	 * This is done by making use of MC's exception when particle count is 0,
	 * and spawning the calculated number of particles each player should see
	 * one by one.
	 */
	public boolean mIsDirectional;

	/*
	 * When using directional movement,
	 * this randomises between -mExtraVariance and +mExtraVariance,
	 * applied to mExtra to individually vary values.
	 */
	public double mExtraVariance;

	/*
	 * Share the same delta for X, Y and Z.
	 * Use default data.
	 * Use default directional args.
	 */
	public PartialParticle(@NotNull Particle particle, @NotNull Location location, int count, double delta, double extra) {
		this(particle, location, count, delta, delta, delta, extra);
	}

	/*
	 * Share the same delta for X, Y and Z.
	 * Use default directional args.
	 */
	public PartialParticle(@NotNull Particle particle, @NotNull Location location, int count, double delta, double extra, @Nullable Object data) {
		this(particle, location, count, delta, delta, delta, extra, data);
	}

	/*
	 * Share the same delta for X, Y and Z.
	 */
	public PartialParticle(@NotNull Particle particle, @NotNull Location location, int count, double delta, double extra, @Nullable Object data, boolean isDirectional, double extraVariance) {
		this(particle, location, count, delta, delta, delta, extra, data, isDirectional, extraVariance);
	}

	/*
	 * Use default data.
	 * Use default directional args.
	 */
	public PartialParticle(@NotNull Particle particle, @NotNull Location location, int count, double deltaX, double deltaY, double deltaZ, double extra) {
		this(particle, location, count, deltaX, deltaY, deltaZ, extra, null);
	}

	/*
	 * Use default directional args.
	 */
	public PartialParticle(@NotNull Particle particle, @NotNull Location location, int count, double deltaX, double deltaY, double deltaZ, double extra, @Nullable Object data) {
		this(particle, location, count, deltaX, deltaY, deltaZ, extra, data, false, 0);
	}

	public PartialParticle(
		@NotNull Particle particle,
		@NotNull Location location,
		int count,
		double offsetX,
		double offsetY,
		double offsetZ,
		double extra,
		@Nullable Object data,
		boolean isDirectional,
		double extraVariance
	) {
		mParticle = particle;
		mLocation = location;
		mCount = count;
		mDeltaX = offsetX;
		mDeltaY = offsetY;
		mDeltaZ = offsetZ;
		mExtra = extra;
		mData = data;
		mIsDirectional = isDirectional;
		mExtraVariance = extraVariance;
	}

	/*
	 * Returns a good value to use for the X and Z deltas,
	 * if you want most particles to start within the specified entity's width.
	 *
	 * When spawning normally,
	 * particle deltas use Gaussian distribution to vary from mLocation,
	 * up to delta * 8 away, but mostly within the closer half.
	 */
	public static double getWidthDelta(@NotNull Entity entity) {
		return getWidthDelta(entity.getWidth());
	}

	/*
	 * Returns a good value to use for the X and Z deltas,
	 * if you want most particles to start within the specified width.
	 */
	public static double getWidthDelta(double entityWidth) {
		return entityWidth / 4;
	}

	/*
	 * Returns a good value to use for the Y delta,
	 * if you want most particles to start within the specified entity's height.
	 */
	public static double getHeightDelta(@NotNull Entity entity) {
		return getHeightDelta(entity.getHeight());
	}

	/*
	 * Returns a good value to use for the Y delta,
	 * if you want most particles to start within the specified height.
	 */
	public static double getHeightDelta(double entityHeight) {
		return entityHeight / 4;
	}

	/*
	 * Use default isPassive.
	 */
	public @NotNull PartialParticle spawnAsPlayer(
		@NotNull Player sourcePlayer
	) {
		return spawnAsPlayer(sourcePlayer, false);
	}

	/*
	 * Spawns particles at mLocation for each nearby player,
	 * based on individual particle multiplier settings.
	 *
	 * Specify a sourcePlayer so we know which multiplier to use on each player;
	 * the OWN_ multipliers are used on the player causing the particles,
	 * while the OTHER_ multipliers are used for other players seeing his/her
	 * particles.
	 *
	 * Specify isPassive as false for active particles
	 * (eg Mana Lance ability, Spark enchant),
	 * or as true for passive particles (eg Gilded enchant).
	 */
	public @NotNull PartialParticle spawnAsPlayer(
		@NotNull Player sourcePlayer,
		boolean isPassive
	) {
		return forEachNearbyPlayer(
			(@NotNull Player player) -> {
				if (player == sourcePlayer) {
					if (isPassive) {
						spawnForPlayer(player, Source.OWN_PASSIVE);
					} else {
						spawnForPlayer(player, Source.OWN_ACTIVE);
					}
				} else {
					if (isPassive) {
						spawnForPlayer(player, Source.OTHER_PASSIVE);
					} else {
						spawnForPlayer(player, Source.OTHER_ACTIVE);
					}
				}
			}
		);
	}

	/*
	 * Spawns particles at mLocation for each nearby player,
	 * based on individual enemy particle multiplier settings.
	 */
	public @NotNull PartialParticle spawnAsEnemy() {
		return spawnForPlayers(Source.ENEMY);
	}

	/*
	 * Spawns particles at mLocation for each nearby player,
	 * based on individual boss particle multiplier settings.
	 */
	public @NotNull PartialParticle spawnAsBoss() {
		return spawnForPlayers(Source.BOSS);
	}

	/*
	 * Spawns particles at mLocation for each nearby player,
	 * with no partial multiplier applied
	 * (always spawns the full mCount amount).
	 */
	public @NotNull PartialParticle spawnFull() {
		forEachSpawn(
			(@NotNull PartialParticle referenceData) -> {
				mLocation.getWorld().spawnParticle(
					referenceData.mParticle,
					referenceData.mLocation,
					referenceData.mCount,
					referenceData.mDeltaX,
					referenceData.mDeltaY,
					referenceData.mDeltaZ,
					referenceData.mExtra,
					referenceData.mData
				);
			}
		);

		return this;
	}

	private @NotNull PartialParticle spawnForPlayers(
		@NotNull Source source
	) {
		return forEachNearbyPlayer(
			(@NotNull Player player) -> spawnForPlayer(player, source)
		);
	}

	private @NotNull PartialParticle forEachNearbyPlayer(
		@NotNull Consumer<@NotNull Player> consumer
	) {
		for (@NotNull Player player : mLocation.getNearbyPlayers(30)) {
			consumer.accept(player);
		}

		return this;
	}

	private @NotNull PartialParticle spawnForPlayer(
		@NotNull Player player,
		@NotNull Source source
	) {
		// Ceil so that when count & multiplier are above 0, players will always see at least 1 particle
		int partialCount = (int)Math.ceil(
			mCount * PlayerData.getParticleMultiplier(player, source)
		);
		forEachSpawn(
			partialCount,
			(@NotNull PartialParticle referenceData) -> {
				player.spawnParticle(
					referenceData.mParticle,
					referenceData.mLocation,
					referenceData.mCount,
					referenceData.mDeltaX,
					referenceData.mDeltaY,
					referenceData.mDeltaZ,
					referenceData.mExtra,
					referenceData.mData
				);
			}
		);

		return this;
	}

	private @NotNull PartialParticle forEachSpawn(
		@NotNull Consumer<@NotNull PartialParticle> consumer
	) {
		return forEachSpawn(mCount, consumer);
	}

	private @NotNull PartialParticle forEachSpawn(
		int amountToSpawn,
		@NotNull Consumer<@NotNull PartialParticle> consumer
	) {
		PartialParticle consumerReferenceData = new PartialParticle(
			mParticle,
			mLocation,
			amountToSpawn,
			mDeltaX,
			mDeltaY,
			mDeltaZ,
			mExtra,
			mData
		);
		// No need to also copy mIsDirectional & mExtraVariance,
		// this is just for the consumer to easily use each loop's modified
		// values without messing with temporarily modifying this object itself

		// When not using directional movement,
		// just need to spawn normally 1 time in the world - the amountToSpawn
		// - unless amountToSpawn is 0, eg multiplier to hide all.
		// All players will see it & MC will handle the variance/drift
		int loops = (amountToSpawn == 0) ? 0 : 1;
		if (mIsDirectional) {
			// When using directional movement,
			// we need to loop amountToSpawn times to spawn amountToSpawn
			// particles in the world, one by one.
			// We set the mCount to 0 to use the MC exception,
			// and provide new random mDeltas (& mExtras) with each loop
			loops = amountToSpawn;
			consumerReferenceData.mCount = 0;
		}

		for (int i = 0; i < loops; i++) {
			if (mIsDirectional) {
				consumerReferenceData.mDeltaX = FastUtils.randomDoubleInRange(-mDeltaX, mDeltaX);
				consumerReferenceData.mDeltaY = FastUtils.randomDoubleInRange(-mDeltaY, mDeltaY);
				consumerReferenceData.mDeltaZ = FastUtils.randomDoubleInRange(-mDeltaZ, mDeltaZ);
				if (mExtraVariance != 0) {
					consumerReferenceData.mExtra = mExtra + FastUtils.randomDoubleInRange(-mExtraVariance, mExtraVariance);
				}
			}
			consumer.accept(consumerReferenceData);
		}

		return this;
	}



	public enum Source {
		OWN_PASSIVE(Objectives.PARTICLES_OWN_PASSIVE),
		OWN_ACTIVE(Objectives.PARTICLES_OWN_ACTIVE),
		OTHER_PASSIVE(Objectives.PARTICLES_OTHER_PASSIVE),
		OTHER_ACTIVE(Objectives.PARTICLES_OTHER_ACTIVE),
		ENEMY(Objectives.PARTICLES_ENEMY),
		BOSS(Objectives.PARTICLES_BOSS);

		public @NotNull final String mObjectiveName;

		Source(@NotNull String objectiveName) {
			mObjectiveName = objectiveName;
		}
	}
}