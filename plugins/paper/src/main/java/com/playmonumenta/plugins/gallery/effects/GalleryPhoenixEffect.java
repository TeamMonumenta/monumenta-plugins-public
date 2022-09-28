package com.playmonumenta.plugins.gallery.effects;

import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.gallery.GalleryGame;
import com.playmonumenta.plugins.gallery.GalleryManager;
import com.playmonumenta.plugins.gallery.GalleryPlayer;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.EntityEffect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class GalleryPhoenixEffect extends GalleryEffect {
	/**
	 * - When you are about to die,  regenerate to full health and refresh all your cooldowns. (Can be bought only 3 times total, and only once for wave)
	 */

	public static final String SCOREBOARD_PHOENIX_BOUGHT_ROUND = "PhoenixBoughtRound";
	public static final String SCOREBOARD_PHOENIX_BOUGHT_COUNT = "PhoenixBoughtCount";
	private static final int MAX_PER_ROUND_BUYABLE = 1;
	private static final int MAX_BOUGHT_COUNT = 3;

	public GalleryPhoenixEffect() {
		super(GalleryEffectType.PHOENIX);
	}

	@Override
	public void playerGainEffect(GalleryPlayer player) {
		super.playerGainEffect(player);
		GalleryGame game = player.getGame();
		Player player1 = player.getPlayer();

		if (player.isOnline() && game != null) {
			ScoreboardUtils.setScoreboardValue(player1, SCOREBOARD_PHOENIX_BOUGHT_ROUND, game.getCurrentRound());
			ScoreboardUtils.setScoreboardValue(player1, SCOREBOARD_PHOENIX_BOUGHT_COUNT, ScoreboardUtils.getScoreboardValue(player1, SCOREBOARD_PHOENIX_BOUGHT_COUNT).orElse(0) + 1);
		}
	}

	@Override public boolean canBuy(GalleryPlayer player) {
		GalleryGame game = player.getGame();
		Player player1 = player.getPlayer();

		if (player.isOnline() && game != null) {
			int lastTimeBought = ScoreboardUtils.getScoreboardValue(player1, SCOREBOARD_PHOENIX_BOUGHT_ROUND).orElse(0);
			int countBought = ScoreboardUtils.getScoreboardValue(player1, SCOREBOARD_PHOENIX_BOUGHT_COUNT).orElse(0);
			return lastTimeBought - game.getCurrentRound() < MAX_PER_ROUND_BUYABLE && MAX_BOUGHT_COUNT >= countBought;
		}
		return false;
	}

	@Override public void onPlayerFatalHurt(GalleryPlayer player, DamageEvent event, LivingEntity enemy) {
		player.getPlayer().playEffect(EntityEffect.TOTEM_RESURRECT);
		event.setCancelled(true);
		PlayerUtils.healPlayer(GalleryManager.mPlugin, player.getPlayer(), EntityUtils.getMaxHealth(player.getPlayer()));
		GalleryManager.mPlugin.mTimers.updateCooldowns(player.getPlayer(), 0);
		clear(player);
	}
}
