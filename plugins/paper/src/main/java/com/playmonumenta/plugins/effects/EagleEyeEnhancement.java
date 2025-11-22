package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.cosmetics.skills.scout.EagleEyeCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.managers.GlowingManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

public class EagleEyeEnhancement extends Effect {
	public static final String effectID = "EagleEyeEnhancement";

	private final double mDamageBoost;
	private int mHits;
	private final Player mPlayer;
	private final EagleEyeCS mCosmetic;

	public EagleEyeEnhancement(double damage, int hits, int duration, Player player, EagleEyeCS cosmetic) {
		super(duration, effectID);
		mDamageBoost = damage;
		mHits = hits;
		mPlayer = player;
		mCosmetic = cosmetic;
	}

	@Override
	public void onHurt(LivingEntity livingEntity, DamageEvent event) {
		DamageEvent.DamageType type = event.getType();
		if (type == DamageEvent.DamageType.TRUE || type == DamageEvent.DamageType.OTHER || type == DamageEvent.DamageType.AILMENT || type == DamageEvent.DamageType.FIRE || type == DamageEvent.DamageType.POISON || mPlayer != (event.getDamager() instanceof Projectile ? ((Projectile) event.getDamager()).getShooter() : event.getDamager())) {
			return;
		}
		mCosmetic.eyeFirstStrike(livingEntity.getWorld(), mPlayer, livingEntity);
		event.updateDamageWithMultiplier(1 + mDamageBoost);
		mHits--;
		if (mHits <= 0) {
			GlowingManager.clear(livingEntity, "EagleEyeEnhancement-" + mPlayer.name());
			clearEffect();
		}
	}

	@Override
	public String toString() {
		return String.format("EagleEyeEnhancement duration:%d", this.getDuration());
	}
}
