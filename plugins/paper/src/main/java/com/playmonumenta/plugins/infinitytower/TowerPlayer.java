package com.playmonumenta.plugins.infinitytower;

import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import org.bukkit.entity.Player;

public class TowerPlayer {

	public final Player mPlayer;
	public final TowerTeam mTeam;

	public TowerPlayer(Player player) {
		mPlayer = player;
		mPlayer.addScoreboardTag(TowerConstants.PLAYER_TAG);
		mPlayer.addScoreboardTag(TowerConstants.TAG_BETWEEN_BATTLE);
		mPlayer.addScoreboardTag(TowerConstants.TAG_BOOK);
		ScoreboardUtils.setScoreboardValue(player, TowerConstants.COIN_SCOREBOARD_NAME, TowerConstants.STARTING_GOLD);
		mTeam = new TowerTeam(mPlayer.getName(), new ArrayList<>());
	}

}
