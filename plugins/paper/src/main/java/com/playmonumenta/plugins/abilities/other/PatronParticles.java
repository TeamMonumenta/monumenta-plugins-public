package com.playmonumenta.plugins.abilities.other;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;



public abstract class PatronParticles<T> extends Ability {
	private final Particle mParticle;
	private final T mParticleData;
	private final String mParticleObjectiveName;
	private final int mMinimumPatreonScore;

	private final boolean mNoSelfParticles;

	public PatronParticles(
		Plugin plugin,
		@Nullable Player player,
		@NotNull Particle particle,
		String particleObjectiveName,
		int minimumPatreonScore
	) {
		this(
			plugin,
			player,
			particle,
			null,
			particleObjectiveName,
			minimumPatreonScore
		);
	}

	public PatronParticles(
		Plugin plugin,
		@Nullable Player player,
		@NotNull Particle particle,
		@Nullable T particleData,
		String particleObjectiveName,
		int minimumPatreonScore
	) {
		super(plugin, player, null);

		mParticle = particle;
		mParticleObjectiveName = particleObjectiveName;
		mMinimumPatreonScore = minimumPatreonScore;
		mParticleData = particleData;

		if (player != null) {
			// Check their scoreboard setting once & save it. PEB mcfunction refreshes class when toggling this
			// Changing effects also refreshes class, meaning canUse() will function fine just checking once
			mNoSelfParticles = PlayerUtils.isNoSelfParticles(player);
		} else {
			mNoSelfParticles = false;
		}
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		PlayerUtils.spawnHideableParticles(
			mNoSelfParticles,
			mPlayer,
			mParticle,
			EntityUtils.getHeightLocation(mPlayer, 0.25),
			5,
			0.25,
			0.01,
			mParticleData
		);
	}

	// AbilityManager creates Ability objects in a specific order with player as null, just for reference
	// It then uses those objects to test if specific passed in players canUse()
	// If so, only then does it construct a new Ability object for that player and go from there
	//
	// TODO This would make more sense to be static perhaps?
	// But a new AbilityInfo is created per Ability & the default canUse() implementation needs that .mInfo for scoreboard name
	@Override
	public boolean canUse(Player player) {
		int particleScore = ScoreboardUtils.getScoreboardValue(player, mParticleObjectiveName);
		int patreonScore = ScoreboardUtils.getScoreboardValue(player, "Patreon");
		return particleScore > 0 && patreonScore >= mMinimumPatreonScore;
	}
}