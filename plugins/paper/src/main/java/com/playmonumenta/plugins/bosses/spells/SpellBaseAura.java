package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SpellBaseAura extends Spell {
	@FunctionalInterface
	public interface ApplyAuraEffect {
		/**
		 * Runs on users when they are within range of the aura
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
	private int mParticleIter; // Number of particle iterations - rolls around between 0 and 20
	private final List<Player> mParticlePlayers; // Players who have opted to get particles

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
		mParticlePlayers = new ArrayList<Player>();
		mParticlesSummoner = null;

		mRadius = (int)Math.max(mDX, Math.max(mDY, mDZ));
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
		mParticlePlayers = new ArrayList<Player>();
		mParticlesSummoner = particlesSummoner;

		mRadius = (int)Math.max(mDX, Math.max(mDY, mDZ));
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
			for (Player player : mParticlePlayers) {
				if (mParticleArg != null) {
					// Generate particles in area
					player.spawnParticle(mParticle, bossLoc, mNumParticles, mParticleDX, mParticleDY, mParticleDZ, mParticleArg);
					// Generate particles immediately around boss
					player.spawnParticle(mParticle, bossLoc.clone().add(0, 1, 0), 2, 1, 1, 1, mParticleArg);
				} else {
					// Generate particles in area
					player.spawnParticle(mParticle, bossLoc, mNumParticles, mParticleDX, mParticleDY, mParticleDZ);
					// Generate particles immediately around boss
					player.spawnParticle(mParticle, bossLoc.clone().add(0, 1, 0), 2, 1, 1, 1);
				}
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

		// Update the group of nearby players who can see the particles every 5s
		if (mParticle != null) {
			mParticleIter++;
			if (mParticleIter >= 20) {
				mParticleIter = 0;

				// Loop through all nearby players and put the ones that don't have
				// the noAuraParticles tag on a list to send particles to them
				mParticlePlayers.clear();
				for (Player player : PlayerUtils.playersInRange(bossLoc, 80, true)) {
					boolean particlesOk = true;
					for (String tag : player.getScoreboardTags()) {
						if (tag.equals("noAuraParticles")) {
							particlesOk = false;
							break;
						}
					}
					if (particlesOk) {
						mParticlePlayers.add(player);
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
