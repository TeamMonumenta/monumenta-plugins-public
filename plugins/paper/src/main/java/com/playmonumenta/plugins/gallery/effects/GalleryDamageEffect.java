package com.playmonumenta.plugins.gallery.effects;

import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.gallery.GalleryPlayer;
import org.bukkit.entity.LivingEntity;

public class GalleryDamageEffect extends GalleryStackableEffect {

	private static final int DAMAGE_EFFECT_MAX_STACK = 5;
	private static final double DAMAGE_EFFECT_PER_STACK = 0.1;

	public GalleryDamageEffect() {
		super(GalleryEffectType.DAMAGE);
	}

	@Override
	public void onPlayerDamage(GalleryPlayer player, DamageEvent event, LivingEntity entity) {
		if (!DamageEvent.DamageType.getUnscalableDamageType().contains(event.getType())) {
			event.setDamage(event.getDamage() * (1.0 + mStacks * DAMAGE_EFFECT_PER_STACK));
		}
	}


	@Override
	public int getMaxStacks() {
		return DAMAGE_EFFECT_MAX_STACK;
	}
}
