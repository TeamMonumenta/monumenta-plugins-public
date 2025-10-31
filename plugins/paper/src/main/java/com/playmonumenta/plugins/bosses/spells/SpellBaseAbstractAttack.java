package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.events.DamageEvent;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public abstract class SpellBaseAbstractAttack extends Spell {

	@FunctionalInterface
	public interface AttackAesthetics {
		void run(LivingEntity boss);
	}

	@FunctionalInterface
	public interface AestheticsImplementation {
		void run();
	}

	@FunctionalInterface
	public interface DamageImplementation {
		void run();
	}

	public final AestheticsImplementation mAestheticsImplementation;
	public final DamageImplementation mDamageImplementation;
	public final Plugin mPlugin;
	public final LivingEntity mBoss;
	public final int mParticleAmount;
	public final int mTelegraphPulses;
	public final int mCastDelay;
	public final int mPulseStartDelay;
	public final double mParticleSpeed;
	public final Particle mParticle;
	public final DamageEvent.DamageType mDamageType;
	public final double mDamage;
	public final boolean mBypassIframes;
	public final boolean mCauseKnockback;
	public final String mAttackName;
	public final Particle mAttackParticle;
	public final AttackAesthetics mAttackAesthetics;

	public SpellBaseAbstractAttack(AestheticsImplementation aestheticsImplementation, DamageImplementation damageImplementation,
	                               int particleAmount, int telegraphPulses, int castDelay, double particleSpeed, Particle particle, DamageEvent.DamageType damageType,
	                               double damage, boolean bypassIframes, boolean causeKnockback, String attackName, Particle attackParticle, Plugin plugin,
	                               LivingEntity boss, AttackAesthetics attackAesthetics) {
		this(aestheticsImplementation, damageImplementation, particleAmount, telegraphPulses, castDelay, 0, particleSpeed,
			particle, damageType, damage, bypassIframes, causeKnockback, attackName, attackParticle, plugin, boss, attackAesthetics);
	}

	public SpellBaseAbstractAttack(AestheticsImplementation aestheticsImplementation, DamageImplementation damageImplementation,
	                               int particleAmount, int telegraphPulses, int castDelay, int pulseStartDelay, double particleSpeed, Particle particle, DamageEvent.DamageType damageType,
	                               double damage, boolean bypassIframes, boolean causeKnockback, String attackName, Particle attackParticle, Plugin plugin,
	                               LivingEntity boss, AttackAesthetics attackAesthetics) {
		mAestheticsImplementation = aestheticsImplementation;
		mDamageImplementation = damageImplementation;
		mParticleAmount = particleAmount;
		mTelegraphPulses = telegraphPulses;
		mCastDelay = castDelay;
		mPulseStartDelay = pulseStartDelay;
		mParticleSpeed = particleSpeed;
		mParticle = particle;
		mDamageType = damageType;
		mDamage = damage;
		mBypassIframes = bypassIframes;
		mCauseKnockback = causeKnockback;
		mAttackName = attackName;
		mAttackParticle = attackParticle;
		mAttackAesthetics = attackAesthetics;
		mPlugin = plugin;
		mBoss = boss;
	}

	@Override
	public void run() {
		// Telegraph the attack
		mAestheticsImplementation.run();

		// Wait delay then run the damage calc, if damage is not 0
		if (mDamage > 0) {
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				// Do damage
				mDamageImplementation.run();
				// Play sweep sound and particles
				mAttackAesthetics.run(mBoss);
			}, mCastDelay);
		}
	}
}
