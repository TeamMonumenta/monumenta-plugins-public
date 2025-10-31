package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class SpellBaseAura extends Spell {
	@FunctionalInterface
	public interface ApplyAuraEffect {
		/**
		 * Runs on users when they are within range of the aura
		 *
		 * @param player The player to affect
		 */
		void run(Player player);
	}

	@FunctionalInterface
	public interface SummonParticles {
		void run(Entity boss);
	}

	private final Entity mBoss;
	private final boolean mCancelable;
	private final double mHeightClamp;
	private final @Nullable ApplyAuraEffect mAuraEffect;
	private final @Nullable SummonParticles mParticlesSummoner;
	private final double mRadius;

	private int mEffectIter = 0; // Number of effect iterations - rolls around between 0 and 2

	public SpellBaseAura(final Entity boss, final double radius, final double heightClamp,
	                     final @Nullable SummonParticles particlesSummoner, final @Nullable ApplyAuraEffect auraEffect,
	                     final boolean stunAffected) {
		mBoss = boss;
		mRadius = radius;
		mHeightClamp = heightClamp;
		mAuraEffect = auraEffect;
		mParticlesSummoner = particlesSummoner;
		mCancelable = stunAffected;
	}

	/*
	 * This spell is designed to be run passively (4x a second).
	 * Effects don't need to be applied to players that frequently -
	 * so only particles run every invocation, effects only one in four
	 */
	@Override
	public void run() {
		if (!mCancelable || (!EntityUtils.isSilenced(mBoss) && !EntityUtils.isStunned(mBoss))) {
			if (mParticlesSummoner != null) {
				mParticlesSummoner.run(mBoss);
			}

			// Apply effects every other pulse (2 Hz)
			if (mAuraEffect != null) {
				mEffectIter++;
				if (mEffectIter >= 2) {
					mEffectIter = 0;

					PlayerUtils.playersInRange(mBoss.getLocation(), mRadius, true).forEach(player -> {
						if (Math.abs(player.getLocation().getY() - mBoss.getLocation().getY()) < mHeightClamp) {
							// Player is within height clamp and radius
							mAuraEffect.run(player);
						}
					});
				}
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}
}
