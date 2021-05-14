package com.playmonumenta.plugins.player;

import com.playmonumenta.plugins.Constants.Objectives;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import org.bukkit.entity.Player;



// A class to be expanded on as a single place to get player settings/data
public class PlayerData {
	public static double getParticleMultiplier(Player player, PartialParticle.Source source) {
		//TODO for a future system to grab player settings per source, maybe PEB GUI "sliders"?
		switch (source) {
			case OWN_PASSIVE:
				// For now, this is the only setting players can change via existing PEB toggle
				// Scrap this in the future for the sliders mentioned above, maybe scoreboard from 0-100 per source
				return player.getScoreboardTags().contains(Objectives.NO_SELF_PARTICLES) ? 0 : 1;
			case OWN_ACTIVE:
			case OTHER_PASSIVE:
			case OTHER_ACTIVE:
			case ENEMY:
			case BOSS:
			default:
				return 1;
		}
	}

	public static int getPatreonDollars(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, Objectives.PATREON_DOLLARS);
	}
}