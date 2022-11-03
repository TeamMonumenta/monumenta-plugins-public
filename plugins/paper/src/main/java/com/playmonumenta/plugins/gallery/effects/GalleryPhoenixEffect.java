package com.playmonumenta.plugins.gallery.effects;

import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.gallery.GalleryGame;
import com.playmonumenta.plugins.gallery.GalleryManager;
import com.playmonumenta.plugins.gallery.GalleryPlayer;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.ChatColor;
import org.bukkit.EntityEffect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

public class GalleryPhoenixEffect extends GalleryEffect {
	/**
	 * - When you are about to die, regenerate to full health and refresh all your cooldowns. (Can be bought only 3 times total, and only once for wave)
	 */

	public static final String SCOREBOARD_PHOENIX_BOUGHT_ROUND = "PhoenixBoughtRound";
	public static final String SCOREBOARD_PHOENIX_BOUGHT_COUNT = "PhoenixBoughtCount";
	private static final int MAX_PER_ROUND_BUYABLE = 1;
	private static final int MAX_BOUGHT_COUNT = 1;

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

	@Override public void tick(GalleryPlayer player, boolean oneSecond, boolean twoHertz, int ticks) {
		GalleryManager.mPlugin.mEffectManager.addEffect(player.getPlayer(), "Gallery" + mType.getRealName(), new Effect(20, "Gallery" + mType.getRealName()) {
			@Override public String toString() {
				return "Gallery" + mType.getRealName();
			}

			@Override public @Nullable String getDisplay() {
				return ChatColor.GOLD + mType.getRealName();
			}
		});
	}

	@Override public boolean canBuy(GalleryPlayer player) {
		GalleryGame game = player.getGame();
		Player player1 = player.getPlayer();

		if (player.isOnline() && game != null) {
			int countBought = ScoreboardUtils.getScoreboardValue(player1, SCOREBOARD_PHOENIX_BOUGHT_COUNT).orElse(0);
			return MAX_BOUGHT_COUNT > countBought;
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
