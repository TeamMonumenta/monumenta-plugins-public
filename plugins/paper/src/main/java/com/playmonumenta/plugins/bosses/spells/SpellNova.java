package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class SpellNova extends SpellBaseAoE {
	public final String mSpellName;
	protected final ParticlesList mParticleAir;
	protected final ParticlesList mParticleLoad;
	protected final EntityTargets mTargets;
	protected final ParticlesList mParticleExplode;
	protected final SoundsList mSoundCast;
	protected final EffectsList mEffects;

	protected final int DAMAGE;
	protected final boolean CAN_BLOCK;
	protected final double DAMAGE_PERCENTAGE;

	public SpellNova(Plugin plugin, LivingEntity launcher, int radius, String name, int duration, int cooldown, boolean canMoveWhileCasting, boolean needLineOfSight,
	                 Sound chargeSound, float soundVolume, int soundDensity, ParticlesList particleAir, ParticlesList particleLoad, ParticlesList particleExplode,
	                 EntityTargets entityTargets, SoundsList soundCast, int damageAmount) {
		this(plugin, launcher, radius, name, duration, cooldown, canMoveWhileCasting, needLineOfSight,
			chargeSound, soundVolume, soundDensity, particleAir, particleLoad, particleExplode,
			entityTargets, soundCast, EffectsList.EMPTY, damageAmount, false, 0.0);
	}

	public SpellNova(Plugin plugin, LivingEntity launcher, int radius, String name, int duration, int cooldown, boolean canMoveWhileCasting, boolean needLineOfSight,
	                 Sound chargeSound, float soundVolume, int soundDensity, ParticlesList particleAir, ParticlesList particleLoad, ParticlesList particleExplode,
	                 EntityTargets entityTargets, SoundsList soundCast, EffectsList effectsList, int damageAmount, boolean canBlock, double damagePercentage) {
		super(plugin, launcher, radius, duration, cooldown, canMoveWhileCasting, needLineOfSight, chargeSound, soundVolume, soundDensity);

		mSpellName = name;
		mParticleAir = particleAir;
		mParticleLoad = particleLoad;
		mTargets = entityTargets;
		mParticleExplode = particleExplode;
		mSoundCast = soundCast;
		mEffects = effectsList;
		DAMAGE = damageAmount;
		CAN_BLOCK = canBlock;
		DAMAGE_PERCENTAGE = damagePercentage;
	}

	@Override
	protected void chargeAuraAction(Location loc) {
		mParticleAir.spawn(mLauncher, loc, mTargets.getRange() / 2, mTargets.getRange() / 2, mTargets.getRange() / 2, 0.05);
	}

	@Override
	protected void chargeCircleAction(Location loc, double radius) {
		mParticleLoad.spawn(mLauncher, particle -> new PPCircle(particle, loc, radius).delta(0.25));
	}

	@Override
	protected void outburstAction(Location loc) {
		mSoundCast.play(loc, 1.5f, 0.65f);
	}

	@Override
	protected void circleOutburstAction(Location loc, double radius) {
		mParticleExplode.spawn(mLauncher, particle -> new PPCircle(particle, loc, radius).delta(0.2).extra(0.2));
	}

	@Override
	protected void dealDamageAction(Location loc) {
		for (LivingEntity target : mTargets.getTargetsList(mLauncher)) {
			if (DAMAGE > 0) {
				if (CAN_BLOCK) {
					BossUtils.blockableDamage(mLauncher, target, DamageEvent.DamageType.MAGIC, DAMAGE, mSpellName, mLauncher.getLocation(), mEffects.mEffectList());
				} else {
					DamageUtils.damage(mLauncher, target, DamageEvent.DamageType.MAGIC, DAMAGE, null, false, true, mSpellName);
				}
			}

			if (DAMAGE_PERCENTAGE > 0.0) {
				BossUtils.bossDamagePercent(mLauncher, target, DAMAGE_PERCENTAGE, CAN_BLOCK ? mLauncher.getLocation() : null, mSpellName, mEffects.mEffectList());
			}
			mEffects.apply(target, mLauncher);
		}
	}
}
