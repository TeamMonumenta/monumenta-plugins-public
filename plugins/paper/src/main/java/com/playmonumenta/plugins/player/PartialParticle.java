package com.playmonumenta.plugins.player;

import com.destroystokyo.paper.ParticleBuilder;
import com.playmonumenta.plugins.Constants.Objectives;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;


public class PartialParticle {
	// https://minecraft.fandom.com/wiki/Commands/particle#Arguments
	// https://papermc.io/javadocs/paper/1.16/org/bukkit/entity/Player.html#spawnParticle-org.bukkit.Particle-org.bukkit.Location-int-double-double-double-double-T-
	public Particle mParticle;
	public Location mLocation;
	public int mCount;
	public double mDeltaX;
	public double mDeltaY;
	public double mDeltaZ;
	public double mExtra;
	public @Nullable Object mData;

	/*
	 * Set to true to use mDelta values to move particles specifically in that
	 * relative direction.
	 * Set to false to use mDeltas for normal location randomisation.
	 */
	public boolean mDirectionalMode;

	/*
	 * Set to non-0 to randomly vary individual particles' mExtra values,
	 * by +- mExtraVariance.
	 */
	public double mExtraVariance;

	/*
	 * Set to true for players to always see at least 1 particle if their
	 * particle multiplier setting is not completely off
	 * (eg for 20% multipler against 3 mCount, that player would see 1 particle).
	 * Set to false to determine by precise chance whether or not < 1 count
	 * spawns 1 particle (eg 20% of 3 mCount would be 0.6,
	 * player has 60% chance to see 1 particle, 40% chance for nothing).
	 */
	public boolean mMinimumMultiplier;

	/*
	 * Whether to randomise between negative mDelta or 0, and 0 or mDelta,
	 * for each axis, for individual particles' mDelta values.
	 */
	public boolean mVaryPositiveX = false;
	public boolean mVaryPositiveY = false;
	public boolean mVaryPositiveZ = false;
	public boolean mVaryNegativeX = false;
	public boolean mVaryNegativeY = false;
	public boolean mVaryNegativeZ = false;

	/*
	 * Share the same delta for X, Y and Z.
	 * Use default data.
	 * Use default directional/variance settings.
	 * Use default multiplier mode.
	 */
	public PartialParticle(Particle particle, Location location, int count, double delta, double extra) {
		this(particle, location, count, delta, delta, delta, extra);
	}

	/*
	 * Share the same delta for X, Y and Z.
	 * Use default directional/variance settings.
	 * Use default multiplier mode.
	 */
	public PartialParticle(Particle particle, Location location, int count, double delta, double extra, @Nullable Object data) {
		this(particle, location, count, delta, delta, delta, extra, data);
	}

	/*
	 * Share the same delta for X, Y and Z.
	 * Use default multiplier mode.
	 */
	public PartialParticle(Particle particle, Location location, int count, double delta, double extra, @Nullable Object data, boolean directionalMode, double extraVariance) {
		this(particle, location, count, delta, delta, delta, extra, data, directionalMode, extraVariance);
	}

	/*
	 * Share the same delta for X, Y and Z.
	 */
	public PartialParticle(Particle particle, Location location, int count, double delta, double extra, @Nullable Object data, boolean directionalMode, double extraVariance, boolean minimumMultiplier) {
		this(particle, location, count, delta, delta, delta, extra, data, directionalMode, extraVariance, minimumMultiplier);
	}

	/*
	 * Use default data.
	 * Use default directional/variance settings.
	 * Use default multiplier mode.
	 */
	public PartialParticle(Particle particle, Location location, int count, double deltaX, double deltaY, double deltaZ, double extra) {
		this(particle, location, count, deltaX, deltaY, deltaZ, extra, null);
	}

	/*
	 * Use default directional/variance settings.
	 * Use default multiplier mode.
	 */
	public PartialParticle(Particle particle, Location location, int count, double deltaX, double deltaY, double deltaZ, double extra, @Nullable Object data) {
		this(particle, location, count, deltaX, deltaY, deltaZ, extra, data, false, 0);
	}

	/*
	 * Use default multiplier mode.
	 */
	public PartialParticle(Particle particle, Location location, int count, double deltaX, double deltaY, double deltaZ, double extra, @Nullable Object data, boolean directionalMode, double extraVariance) {
		this(particle, location, count, deltaX, deltaY, deltaZ, extra, data, directionalMode, extraVariance, true);
	}

	public PartialParticle(
		Particle particle,
		Location location,
		int count,
		double deltaX,
		double deltaY,
		double deltaZ,
		double extra,
		@Nullable Object data,
		boolean directionalMode,
		double extraVariance,
		boolean minimumMultiplier
	) {
		mParticle = particle;
		mLocation = location;
		mCount = count;
		mDeltaX = deltaX;
		mDeltaY = deltaY;
		mDeltaZ = deltaZ;
		mExtra = extra;
		mData = data;
		mDirectionalMode = directionalMode;
		mExtraVariance = extraVariance;
		mMinimumMultiplier = minimumMultiplier;
	}

	/*
	 * Returns a good value to use for the X and Z deltas,
	 * if you want most particles to start within the specified entity's width.
	 */
	public static double getWidthDelta(Entity entity) {
		return getWidthDelta(entity.getWidth());
	}

	/*
	 * Returns a good value to use for the X and Z deltas,
	 * if you want most particles to start within the specified width.
	 */
	public static double getWidthDelta(double entityWidth) {
		return getDelta(entityWidth);
	}

	/*
	 * Returns a good value to use for the Y delta,
	 * if you want most particles to start within the specified entity's height.
	 */
	public static double getHeightDelta(Entity entity) {
		return getHeightDelta(entity.getHeight());
	}

	/*
	 * Returns a good value to use for the Y delta,
	 * if you want most particles to start within the specified height.
	 */
	public static double getHeightDelta(double entityHeight) {
		return getDelta(entityHeight);
	}

	/*
	 * Returns a good value to use for a delta,
	 * if you want most particles to start within the specified length.
	 */
	public static double getDelta(double length) {
		// When spawning normally, particles randomly vary their location
		// following Gaussian distribution, up to delta * 8 away,
		// but mostly within the closer half.
		return length / 4;
	}

	/*
	 * Use default delta variance group.
	 */
	public void setDeltaVariance(
		boolean deltaVariance
	) {
		setDeltaVariance(DeltaVarianceGroup.VARY_ALL, deltaVariance);
	}

	/*
	 * Convenience method to update multiple mVary settings at once.
	 */
	public void setDeltaVariance(
		DeltaVarianceGroup deltaVarianceGroup,
		boolean deltaVariance
	) {
		boolean setAll = DeltaVarianceGroup.VARY_ALL.equals(deltaVarianceGroup);
		if (setAll || DeltaVarianceGroup.VARY_X.equals(deltaVarianceGroup)) {
			mVaryPositiveX = deltaVariance;
			mVaryNegativeX = deltaVariance;
		}
		if (setAll || DeltaVarianceGroup.VARY_Y.equals(deltaVarianceGroup)) {
			mVaryPositiveY = deltaVariance;
			mVaryNegativeY = deltaVariance;
		}
		if (setAll || DeltaVarianceGroup.VARY_Z.equals(deltaVarianceGroup)) {
			mVaryPositiveZ = deltaVariance;
			mVaryNegativeZ = deltaVariance;
		}
	}

	/*
	 * Whether extra variance has been enabled.
	 */
	public boolean isExtraVaried() {
		return mExtraVariance != 0;
	}

	/*
	 * Use default delta variance group.
	 */
	public boolean isDeltaVaried() {
		return isDeltaVaried(DeltaVarianceGroup.VARY_ALL);
	}

	/*
	 * Whether delta variance for the specified group has been enabled.
	 */
	public boolean isDeltaVaried(DeltaVarianceGroup deltaVarianceGroup) {
		if (DeltaVarianceGroup.VARY_ALL.equals(deltaVarianceGroup)) {
			return (
				isDeltaVaried(DeltaVarianceGroup.VARY_X)
					|| isDeltaVaried(DeltaVarianceGroup.VARY_Y)
					|| isDeltaVaried(DeltaVarianceGroup.VARY_Z)
			);
		} else if (DeltaVarianceGroup.VARY_X.equals(deltaVarianceGroup)) {
			return mVaryNegativeX || mVaryPositiveX;
		} else if (DeltaVarianceGroup.VARY_Y.equals(deltaVarianceGroup)) {
			return mVaryNegativeY || mVaryPositiveY;
		} else if (DeltaVarianceGroup.VARY_Z.equals(deltaVarianceGroup)) {
			return mVaryNegativeZ || mVaryPositiveZ;
		} else {
			return false;
		}
	}

	public PartialParticle location(Location loc) {
		mLocation = loc;
		return this;
	}

	/*
	 * Use default isPassive.
	 */
	public PartialParticle spawnAsPlayer(
		Player sourcePlayer
	) {
		return spawnAsPlayer(sourcePlayer, false);
	}

	/*
	 * Spawns particles for each nearby player,
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
	public PartialParticle spawnAsPlayer(
		Player sourcePlayer,
		boolean isPassive
	) {
		return forEachNearbyPlayer(
			(Player player) -> {
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
	 * Spawns particles for each nearby player,
	 * based on individual enemy particle multiplier settings.
	 */
	public PartialParticle spawnAsEnemy() {
		return spawnForPlayers(Source.ENEMY);
	}

	/*
	 * Spawns particles for each nearby player,
	 * based on individual boss particle multiplier settings.
	 */
	public PartialParticle spawnAsBoss() {
		return spawnForPlayers(Source.BOSS);
	}

	/*
	 * Spawns particles for each nearby player,
	 * with no partial multiplier applied
	 * (always spawns the full mCount amount).
	 */
	public PartialParticle spawnFull() {
		return spawnForPlayers(Source.FULL);
	}

	/*
	 * Called once per nearby player,
	 * with packaged up values for you to use them to spawn particles in your
	 * desired pattern.
	 *
	 * This is likely the method you wish to override when subclassing.
	 * You have the chance to apply custom logic and then call
	 * spawnUsingSettings() with differnt packagedValues,
	 * as many times as needed.
	 */
	protected void doSpawn(ParticleBuilder packagedValues) {
		spawnUsingSettings(packagedValues);
	}

	/*
	 * Spawns the specified packagedValues normally,
	 * or if directional mode and/or delta/extra variance are enabled,
	 * applies them to a clone of the specified packagedValues,
	 * looping internally as needed.
	 */
	protected void spawnUsingSettings(
		ParticleBuilder packagedValues
	) {
		if (!(mDirectionalMode || isDeltaVaried() || isExtraVaried())) {
			packagedValues.spawn();
		} else {
			ParticleBuilder variedClone = new ParticleBuilder(packagedValues.particle());
			variedClone.location(packagedValues.location());
			variedClone.extra(packagedValues.extra());
			variedClone.data(packagedValues.data());
			variedClone.receivers(packagedValues.receivers());

			int loops = packagedValues.count();
			if (mDirectionalMode) {
				// If directional mode, need to spawn one by one.
				// We set count to 0 to use MC's directional movement exception
				variedClone.count(0);
			} else {
				// Otherwise, if want to vary delta or extra,
				// also need to loop to spawn 1 each time
				variedClone.count(1);
			}

			for (int i = 0; i < loops; i++) {
				double variedDeltaX = packagedValues.offsetX();
				double variedDeltaY = packagedValues.offsetY();
				double variedDeltaZ = packagedValues.offsetZ();
				if (isDeltaVaried(DeltaVarianceGroup.VARY_X)) {
					variedDeltaX = FastUtils.randomDoubleInRange(
						mVaryNegativeX ? -variedDeltaX : 0,
						mVaryPositiveX ? variedDeltaX : 0
					);
				}
				if (isDeltaVaried(DeltaVarianceGroup.VARY_Y)) {
					variedDeltaY = FastUtils.randomDoubleInRange(
						mVaryNegativeY ? -variedDeltaY : 0,
						mVaryPositiveY ? variedDeltaY : 0
					);
				}
				if (isDeltaVaried(DeltaVarianceGroup.VARY_Z)) {
					variedDeltaZ = FastUtils.randomDoubleInRange(
						mVaryNegativeZ ? -variedDeltaZ : 0,
						mVaryPositiveZ ? variedDeltaZ : 0
					);
				}
				variedClone.offset(variedDeltaX, variedDeltaY, variedDeltaZ);

				if (isExtraVaried()) {
					variedClone.extra(
						packagedValues.extra() + FastUtils.randomDoubleInRange(-mExtraVariance, mExtraVariance)
					);
				}

				variedClone.spawn();
			}
		}
	}

	private PartialParticle spawnForPlayers(
		Source source
	) {
		return forEachNearbyPlayer(
			(Player player) -> spawnForPlayer(player, source)
		);
	}

	private PartialParticle forEachNearbyPlayer(
		Consumer<Player> playerAction
	) {
		for (Player player : mLocation.getNearbyPlayers(100)) {
			playerAction.accept(player);
		}

		return this;
	}

	private void spawnForPlayer(
		Player player,
		Source source
	) {
		double multipliedCount = mCount * PlayerData.getParticleMultiplier(player, source);
		if (multipliedCount == 0) {
			return;
		}

		int partialCount;
		if (mMinimumMultiplier || multipliedCount >= 1) {
			partialCount = (int)Math.ceil(multipliedCount);
		} else {
			// If don't want minimum multiplier (don't assume ceil 1 particle),
			// and count is a double under 1,
			// we randomise whether to see that 1 particle
			if (FastUtils.RANDOM.nextDouble() < multipliedCount) {
				partialCount = 1;
			} else {
				// partialCount of 0
				return;
			}
		}

		ParticleBuilder packagedValues = new ParticleBuilder(mParticle);
		packagedValues.location(mLocation);
		packagedValues.count(partialCount);
		packagedValues.offset(mDeltaX, mDeltaY, mDeltaZ);
		packagedValues.extra(mExtra);
		packagedValues.data(mData);

		packagedValues.receivers(player);

		doSpawn(packagedValues);
	}



	public enum DeltaVarianceGroup {
		VARY_ALL,
		VARY_X,
		VARY_Y,
		VARY_Z;
	}

	public enum Source {
		OWN_PASSIVE(Objectives.PP_OWN_PASSIVE),
		OWN_ACTIVE(Objectives.PP_OWN_ACTIVE),
		OTHER_PASSIVE(Objectives.PP_OTHER_PASSIVE),
		OTHER_ACTIVE(Objectives.PP_OTHER_ACTIVE),
		ENEMY(Objectives.PP_ENEMY),
		BOSS(Objectives.PP_BOSS),
		FULL(null);

		public final @Nullable String mObjectiveName;

		Source(@Nullable String objectiveName) {
			mObjectiveName = objectiveName;
		}
	}
}
