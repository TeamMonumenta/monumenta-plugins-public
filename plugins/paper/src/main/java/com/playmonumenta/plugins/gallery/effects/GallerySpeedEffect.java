package com.playmonumenta.plugins.gallery.effects;

import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.gallery.GalleryPlayer;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;

public class GallerySpeedEffect extends GalleryStackableEffect {

	private static final int SPEED_EFFECT_MAX_STACK = 5;
	private static final double SPEED_EFFECT_PER_STACK = 0.1;

	public GallerySpeedEffect() {
		super(GalleryEffectType.SPEED);
	}

	@Override public void playerGainEffect(GalleryPlayer player) {
		super.playerGainEffect(player);
		EntityUtils.addAttribute(player.getPlayer(), Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier("GallerySpeedEffect", SPEED_EFFECT_PER_STACK * mStacks, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
	}

	@Override public void playerLoseEffect(GalleryPlayer player) {
		super.playerLoseEffect(player);
		EntityUtils.removeAttribute(player.getPlayer(), Attribute.GENERIC_MOVEMENT_SPEED, "GallerySpeedEffect");
	}

	@Override
	public void clear(GalleryPlayer player) {
		EffectManager.getInstance().clearEffects(player.getPlayer(), "GallerySpeedEffect");
	}

	@Override public int getMaxStacks() {
		return SPEED_EFFECT_MAX_STACK;
	}
}
