package com.playmonumenta.plugins.gallery.effects;

import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.gallery.GalleryGame;
import com.playmonumenta.plugins.gallery.GalleryManager;
import com.playmonumenta.plugins.gallery.GalleryPlayer;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
	private static final int MAX_BOUGHT_COUNT = 1;

	public GalleryPhoenixEffect() {
		super(GalleryEffectType.PHOENIX);
	}

	@Override
	public void playerGainEffect(GalleryPlayer galleryPlayer) {
		super.playerGainEffect(galleryPlayer);
		GalleryGame game = galleryPlayer.getGame();
		Player player = galleryPlayer.getPlayer();

		if (player != null && player.isOnline() && game != null) {
			ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_PHOENIX_BOUGHT_ROUND, game.getCurrentRound());
			ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_PHOENIX_BOUGHT_COUNT, ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PHOENIX_BOUGHT_COUNT).orElse(0) + 1);
		}
	}

	@Override
	public Component getDisplay() {
		return getDisplayWithoutTime();
	}

	@Override
	public Component getDisplayWithoutTime() {
		return Component.text(mType.getRealName(), NamedTextColor.GOLD);
	}

	@Override
	public boolean canBuy(GalleryPlayer galleryPlayer) {
		GalleryGame game = galleryPlayer.getGame();
		Player player = galleryPlayer.getPlayer();

		if (player != null && player.isOnline() && game != null) {
			int countBought = ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PHOENIX_BOUGHT_COUNT).orElse(0);
			return MAX_BOUGHT_COUNT > countBought;
		}
		return false;
	}

	@Override
	public void onPlayerFatalHurt(GalleryPlayer galleryPlayer, DamageEvent event, @Nullable LivingEntity enemy) {
		Player player = galleryPlayer.getPlayer();
		if (player == null) {
			return;
		}
		player.playEffect(EntityEffect.TOTEM_RESURRECT);
		event.setCancelled(true);
		PlayerUtils.healPlayer(GalleryManager.mPlugin, player, EntityUtils.getMaxHealth(player));
		GalleryManager.mPlugin.mTimers.updateCooldowns(player, 0);
		clear(galleryPlayer);
	}
}
