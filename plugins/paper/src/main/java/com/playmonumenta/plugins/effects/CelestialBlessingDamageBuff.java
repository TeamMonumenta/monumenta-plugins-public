package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.cosmetics.skills.cleric.CelestialBlessingCS;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.cleric.CelestialBlessing.CELESTIAL_BUFF_EXTENSION_DURATION_ENHANCED;
import static com.playmonumenta.plugins.abilities.cleric.CelestialBlessing.PARTICLE_EFFECT_NAME;
import static com.playmonumenta.plugins.abilities.cleric.CelestialBlessing.SPEED_EFFECT_NAME;

public class CelestialBlessingDamageBuff extends PercentDamageDealt {
	public static final String effectID = "CelestialBlessingDamageBuff";

	private final CelestialBlessingCS mCosmetic;
	private final boolean mEnhanced;
	private final Player mPlayer;
	private boolean mMelee;
	private boolean mRanged;
	private boolean mCast;
	private boolean mExtended;

	public CelestialBlessingDamageBuff(int duration, double amount, boolean enhanced, CelestialBlessingCS cosmetic,
	                                   Player player, @Nullable EnumSet<DamageEvent.DamageType> affectedDamageTypes) {
		super(duration, amount, affectedDamageTypes, effectID);
		mEnhanced = enhanced;
		mMelee = false;
		mRanged = false;
		mCast = false;
		mExtended = false;
		mCosmetic = cosmetic;
		mPlayer = player;
	}

	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		super.onDamage(entity, event, enemy);
		if (event.getFlatDamage() >= 1) {
			if (event.getType() == DamageEvent.DamageType.MELEE && event.getDamager() instanceof final Player player && player.getCooledAttackStrength(0.5f) > 0.9) {
				mMelee = true;
			}
			if (event.getType() == DamageEvent.DamageType.PROJECTILE && event.getDamager() instanceof final Projectile projectile && EntityUtils.isAbilityTriggeringProjectile(projectile, true)) {
				mRanged = true;
			}
		}
		extendBuff();
	}

	@Override
	public void onAbilityCast(AbilityCastEvent event, Player player) {
		// Depending on balance might need to remove the fact that cleric counts cbless as a cast
		mCast = true;
		extendBuff();
	}

	private void extendBuff() {
		if (!mExtended && mEnhanced && mMelee && mRanged && mCast) {
			mExtended = true;
			EffectManager manager = EffectManager.getInstance();
			setDuration(getDuration() + CELESTIAL_BUFF_EXTENSION_DURATION_ENHANCED);
			Set<Effect> speed = manager.getEffects(mPlayer, SPEED_EFFECT_NAME);
			if (speed != null) {
				for (Effect e : speed) {
					e.setDuration(e.getDuration() + CELESTIAL_BUFF_EXTENSION_DURATION_ENHANCED);
				}
			}
			Set<Effect> particles = manager.getEffects(mPlayer, PARTICLE_EFFECT_NAME);
			if (particles != null) {
				for (Effect e : particles) {
					e.setDuration(e.getDuration() + CELESTIAL_BUFF_EXTENSION_DURATION_ENHANCED);
				}
			}
			mCosmetic.enhanceExtension(mPlayer);
		}
	}
}
