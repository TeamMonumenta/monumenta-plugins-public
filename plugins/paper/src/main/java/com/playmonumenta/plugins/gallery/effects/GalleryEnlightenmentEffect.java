package com.playmonumenta.plugins.gallery.effects;

import com.playmonumenta.plugins.effects.AbilityCooldownDecrease;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.gallery.GalleryPlayer;
import org.bukkit.entity.Player;

public class GalleryEnlightenmentEffect extends GalleryConsumableEffect {

	public GalleryEnlightenmentEffect() {
		super(GalleryEffectType.ENLIGHTENMENT);
	}

	@Override
	public void tick(GalleryPlayer galleryPlayer, boolean oneSecond, boolean twoHertz, int ticks) {
		super.tick(galleryPlayer, oneSecond, twoHertz, ticks);
		if (twoHertz) {
			Player player = galleryPlayer.getPlayer();
			if (player == null) {
				return;
			}
			EffectManager.getInstance().addEffect(player, "GalleryEnlightenmentEffect", new AbilityCooldownDecrease(20, 0.2).displays(false));
		}
	}
}
