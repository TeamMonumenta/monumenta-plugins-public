package com.playmonumenta.plugins.player;

import com.playmonumenta.plugins.Constants.Objectives;
import com.playmonumenta.plugins.Constants.Tags;
import com.playmonumenta.plugins.player.PartialParticle.Source;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.entity.Player;
import javax.annotation.Nullable;



/*
 * A single place to get player data & settings.
 */
public class PlayerData {
	public static double getParticleMultiplier(
		Player player,
		Source source
	) {
		// For now, this is the only setting players can change via existing PEB toggle
		//TODO Scrap this part in the future once players can slide these in PEB GUI, scoreboard from 0-100 per source
		if (
			Source.OWN_PASSIVE.equals(source)
			&& ScoreboardUtils.checkTag(player, Tags.NO_SELF_PARTICLES)
		) {
			return 0;
		}

		@Nullable String objectiveName = source.mObjectiveName;
		if (objectiveName != null) {
			// Defaults to 100% when value is missing
			int interpretedValue = ScoreboardUtils.getScoreboardValue(player, objectiveName).orElse(100);
			int clampedValue = Math.min(interpretedValue, 500);
			clampedValue = Math.max(clampedValue, 0);
			return clampedValue / 100d;
		} else {
			return 1;
		}
	}

	public static int getPatreonDollars(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, Objectives.PATREON_DOLLARS).orElse(0);
	}
}
