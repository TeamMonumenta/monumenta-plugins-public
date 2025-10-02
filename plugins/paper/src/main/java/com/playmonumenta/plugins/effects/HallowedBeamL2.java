package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.cleric.seraph.HallowedBeamCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

public class HallowedBeamL2 extends Effect {
	public static final String effectID = "HallowedBeamL2";
	private final Player mPlayer;
	private final double mDamage;
	private final double mRadius;
	private int mSeals;
	private final int mSealDuration;
	private final HallowedBeamCS mCosmetic;

	public HallowedBeamL2(int duration, Player player, double damage, double radius, int seals, HallowedBeamCS cosmetic) {
		super(duration, effectID);
		mPlayer = player;
		mDamage = damage;
		mRadius = radius;
		mSeals = seals;
		mSealDuration = duration;
		mCosmetic = cosmetic;
	}

	@Override
	public void entityGainEffect(Entity entity) {
		GlowingManager.startGlowing(entity, mCosmetic.beamGlowColor(), mDuration, GlowingManager.PLAYER_ABILITY_PRIORITY, p -> p.equals(mPlayer), "HallowedBeamGlowing" + mPlayer.getName());
	}

	@Override
	public void onHurt(LivingEntity livingEntity, DamageEvent event) {
		DamageEvent.DamageType type = event.getType();
		boolean fromEtherealAscension = type == DamageEvent.DamageType.MAGIC && event.getAbility() == ClassAbility.ETHEREAL_ASCENSION;
		if ((!fromEtherealAscension && type != DamageEvent.DamageType.PROJECTILE && type != DamageEvent.DamageType.MELEE) || mPlayer != (event.getDamager() instanceof Projectile ? ((Projectile) event.getDamager()).getShooter() : event.getDamager())) {
			return;
		}

		mCosmetic.beamSplash(mPlayer, livingEntity, livingEntity.getLocation(), mRadius);

		EntityUtils.getNearbyMobs(livingEntity.getLocation(), mRadius).forEach(mob ->
			DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, mDamage, ClassAbility.HALLOWED_BEAM_SEAL, true, false));

		mSeals--;
		if (mSeals > 0) {
			mDuration = mSealDuration;
			entityGainEffect(livingEntity);
		} else {
			clearEffect();
			GlowingManager.clear(livingEntity, "HallowedBeamGlowing" + mPlayer.getName());
		}
	}

	@Override
	public String toString() {
		return String.format("HallowedBeamL2 duration:%d", this.getDuration());
	}
}
