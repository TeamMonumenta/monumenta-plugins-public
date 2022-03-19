package com.playmonumenta.plugins.player;

import com.playmonumenta.plugins.Constants.Objectives;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.entity.Player;


/*
 * A single place to get player data & settings.
 */
public class PlayerData {

	public static int MAX_PARTIAL_PARTICLE_VALUE = 200;

	public static double getParticleMultiplier(Player player, ParticleCategory category) {
		String objectiveName = category.mObjectiveName;
		if (objectiveName == null) {
			// Defaults to 100% when value is missing
			return 1;
		}
		int interpretedValue = ScoreboardUtils.getScoreboardValue(player, objectiveName).orElse(100);
		int clampedValue = Math.min(interpretedValue, MAX_PARTIAL_PARTICLE_VALUE);
		clampedValue = Math.max(clampedValue, 0);
		return clampedValue / 100d;
	}

	public static int getPatreonDollars(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, Objectives.PATREON_DOLLARS).orElse(0);
	}
}
