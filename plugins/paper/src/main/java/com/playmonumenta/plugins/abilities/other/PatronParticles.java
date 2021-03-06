package com.playmonumenta.plugins.abilities.other;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.player.PlayerData;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;



public abstract class PatronParticles extends Ability {
	private final @NotNull Particle mParticle;
	private final @NotNull Object mParticleData;
	private final @NotNull String mParticleObjectiveName;
	private final int mMinimumPatreonScore;

	// Skip particle data
	public PatronParticles(@NotNull Plugin plugin, @Nullable Player player, @NotNull Particle particle, @NotNull String particleObjectiveName, int minimumPatreonScore) {
		this(plugin, player, particle, null, particleObjectiveName, minimumPatreonScore);
	}

	public PatronParticles(
		@NotNull Plugin plugin,
		@Nullable Player player,
		@NotNull Particle particle,
		@Nullable Object particleData,
		@NotNull String particleObjectiveName,
		int minimumPatreonScore
	) {
		super(plugin, player, null);

		mParticle = particle;
		mParticleObjectiveName = particleObjectiveName;
		mMinimumPatreonScore = minimumPatreonScore;
		mParticleData = particleData;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		double widthDelta = PartialParticle.getWidthDelta(mPlayer);
		new PartialParticle(
			mParticle,
			LocationUtils.getHeightLocation(mPlayer, 0.25),
			Constants.QUARTER_TICKS_PER_SECOND,
			widthDelta,
			PartialParticle.getHeightDelta(mPlayer) / 2,
			widthDelta,
			0.01,
			mParticleData
		).spawnAsPlayer(mPlayer, true);
	}

	// AbilityManager creates Ability objects in a specific order with player as null, just for reference.
	// It then uses those objects to test if specific passed in players canUse().
	// If so, only then does it construct a new Ability object for that player and go from there
	//
	// TODO This would make more sense to be static perhaps?
	// But a new AbilityInfo is created per Ability & the default canUse() implementation needs that .mInfo for scoreboard name
	@Override
	public boolean canUse(Player player) {
		// Changing to different coloured particles also refreshes class, meaning canUse() will function fine just checking once
		int particleScore = ScoreboardUtils.getScoreboardValue(player, mParticleObjectiveName);
		return (
			particleScore > 0
			&& PlayerData.getPatreonDollars(player) >= mMinimumPatreonScore
		);
	}
}