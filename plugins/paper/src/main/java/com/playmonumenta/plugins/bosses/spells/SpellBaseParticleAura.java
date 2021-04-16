package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.entity.LivingEntity;
import com.playmonumenta.plugins.utils.EntityUtils;

public class SpellBaseParticleAura extends Spell {

	@FunctionalInterface
	public interface ParticleEffect {
		/**
		 * Runs particles
		 */
		void run(LivingEntity boss);
	}


	private LivingEntity mBoss;
	private int mTicksPerIteration;
	private ParticleEffect[] mEffects;
	private int mEffectIter;

	public SpellBaseParticleAura(LivingEntity boss, int ticksPerIteration, ParticleEffect... effects) {
		this.mBoss = boss;
		this.mTicksPerIteration = ticksPerIteration;
		this.mEffects = effects;
		this.mEffectIter = 0;
	}

	@Override
	public void run() {
		mEffectIter++;
		if (mEffectIter >= mTicksPerIteration) {
			mEffectIter = 0;
			if (EntityUtils.isStunned(mBoss) || EntityUtils.isSilenced(mBoss) || EntityUtils.isConfused(mBoss)) {
				return;
			}
			for (ParticleEffect effect : mEffects) {
				effect.run(mBoss);
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}

}
