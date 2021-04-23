package com.playmonumenta.plugins.player;

import com.playmonumenta.plugins.utils.ScoreboardUtils;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;



//TODO to be expanded as a single place to get player settings/data
//
// Rather than "isSomething" or "getSomething" naming which may imply that data is fixed at time of instantiation,
// methods use "checkSomething" naming if they are checked at time of method call
public class PlayerData {
	@NotNull public Player mPlayer;

	public PlayerData(@NotNull Player player) {
		mPlayer = player;
	}

	// Whether player wants to see their own particles.
	public boolean checkSelfParticles() {
		return !mPlayer.getScoreboardTags().contains("noSelfParticles");
	}

	public double checkParticleMultiplier() {
		//TODO for a future system to grab player settings, maybe PEB GUI "slider"?
		return 1;
	}

	public int checkPatreonDollars() {
		return ScoreboardUtils.getScoreboardValue(mPlayer, "Patreon");
	}
}