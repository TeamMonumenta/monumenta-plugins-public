package com.playmonumenta.plugins.gallery.effects;

import com.playmonumenta.plugins.gallery.GalleryPlayer;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

public class GalleryHealthEffect extends GalleryStackableEffect {

	private static final int HEALTH_EFFECT_MAX_STACK = 5;
	private static final double HEALTH_EFFECT_PER_STACK = 0.1;

	public GalleryHealthEffect() {
		super(GalleryEffectType.HEALTH);
	}

	@Override
	public void playerGainEffect(GalleryPlayer galleryPlayer) {
		super.playerGainEffect(galleryPlayer);
		EntityUtils.addAttribute(galleryPlayer.getPlayer(), Attribute.GENERIC_MAX_HEALTH, new AttributeModifier("GalleryHealthEffect", mStacks * HEALTH_EFFECT_PER_STACK, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
	}

	@Override public void playerLoseEffect(GalleryPlayer player) {
		super.playerLoseEffect(player);
		EntityUtils.removeAttribute(player.getPlayer(), Attribute.GENERIC_MAX_HEALTH, "GalleryHealthEffect");
	}

	@Override public int getMaxStacks() {
		return HEALTH_EFFECT_MAX_STACK;
	}

	@Override public void refresh(GalleryPlayer galleryPlayer) {
		Player player = galleryPlayer.getPlayer();
		if (galleryPlayer.isOnline() && player != null) {
			EntityUtils.removeAttribute(player, Attribute.GENERIC_MAX_HEALTH, "GalleryHealthEffect");
			EntityUtils.addAttribute(player, Attribute.GENERIC_MAX_HEALTH, new AttributeModifier("GalleryHealthEffect", mStacks * HEALTH_EFFECT_PER_STACK, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}
	}
}
