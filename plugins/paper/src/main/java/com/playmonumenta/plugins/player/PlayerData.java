package com.playmonumenta.plugins.player;

import com.playmonumenta.plugins.Constants.Objectives;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.entity.Player;


/*
 * A single place to get player data & settings.
 */
public class PlayerData {
	public static int getPatreonDollars(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, Objectives.PATREON_DOLLARS).orElse(0);
	}
}
