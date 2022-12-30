package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
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
	private final double mDX;
	private final double mDY;
	private final double mDZ;
	private final double mParticleDX;
	private final double mParticleDY;
	private final double mParticleDZ;
	private final int mNumParticles;
	private final @Nullable Particle mParticle;
	private final @Nullable Object mParticleArg;
	private final @Nullable ApplyAuraEffect mAuraEffect;
	private final @Nullable SummonParticles mParticlesSummoner;

	private final int mRadius; // Computed maximum of mDX, mDY, mDZ
	private int mEffectIter; // Number of effect iterations - rolls around between 0 and 2

	public SpellBaseAura(Entity boss, double dx, double dy, double dz, int numParticles,
	                     Particle particle, Object particleArg, ApplyAuraEffect auraEffect) {
		mBoss = boss;
		// The "radius" thing is just... a mystery. Dividing by 2 is slightly better?
		mDX = dx;
		mDY = dy;
		mDZ = dz;
		mParticleDX = dx / 2 - 1;
		mParticleDY = dy / 2 - 1;
		mParticleDZ = dz / 2 - 1;
		mNumParticles = numParticles;
		mParticle = particle;
		mParticleArg = particleArg;
		mAuraEffect = auraEffect;
		mParticlesSummoner = null;

		mRadius = (int) Math.max(mDX, Math.max(mDY, mDZ));
		mEffectIter = 0;
	}

	public SpellBaseAura(Entity boss, double dx, double dy, double dz, SummonParticles particlesSummoner, @Nullable ApplyAuraEffect auraEffect) {
		mBoss = boss;
		// The "radius" thing is just... a mystery. Dividing by 2 is slightly better?
		mDX = dx;
		mDY = dy;
		mDZ = dz;
		mParticleDX = dx / 2 - 1;
		mParticleDY = dy / 2 - 1;
		mParticleDZ = dz / 2 - 1;
		mNumParticles = 0;
		mParticle = null;
		mParticleArg = null;
		mAuraEffect = auraEffect;
		mParticlesSummoner = particlesSummoner;

		mRadius = (int) Math.max(mDX, Math.max(mDY, mDZ));
		mEffectIter = 0;
	}

	/*
	 * This spell is designed to be run passively (4x a second).
	 * Effects don't need to be applied to players that frequently -
	 * so only particles run every invocation, effects only one in four
	 */
	@Override
	public void run() {
		Location bossLoc = mBoss.getLocation();

		if (mParticle != null) {
			if (mParticleArg != null) {
				// Generate particles in area
				new PartialParticle(mParticle, bossLoc, mNumParticles, mParticleDX, mParticleDY, mParticleDZ, mParticleArg)
					.conditional(p -> !p.getScoreboardTags().contains("noAuraParticles"))
					.spawnAsEntityActive(mBoss);
				// Generate particles immediately around boss
				new PartialParticle(mParticle, bossLoc.clone().add(0, 1, 0), 2, 1, 1, 1, mParticleArg).spawnAsEntityActive(mBoss);
			} else {
				// Generate particles in area
				new PartialParticle(mParticle, bossLoc, mNumParticles, mParticleDX, mParticleDY, mParticleDZ)
					.conditional(p -> !p.getScoreboardTags().contains("noAuraParticles"))
					.spawnAsEntityActive(mBoss);
				// Generate particles immediately around boss
				new PartialParticle(mParticle, bossLoc.clone().add(0, 1, 0), 2, 1, 1, 1).spawnAsEntityActive(mBoss);
			}
		} else if (mParticlesSummoner != null) {
			//new version using particlesSummoner
			mParticlesSummoner.run(mBoss);
		}

		// Apply effects every other pulse (2 Hz)
		if (mAuraEffect != null) {
			mEffectIter++;
			if (mEffectIter >= 2) {
				mEffectIter = 0;

				for (Player player : PlayerUtils.playersInRange(bossLoc, mRadius, true)) {
					Location playerLoc = player.getLocation();
					if (Math.abs(playerLoc.getX() - bossLoc.getX()) < mDX &&
						    Math.abs(playerLoc.getY() - bossLoc.getY()) < mDY &&
						    Math.abs(playerLoc.getZ() - bossLoc.getZ()) < mDZ) {
						// Player is within range
						mAuraEffect.run(player);
					}
				}
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}
}
