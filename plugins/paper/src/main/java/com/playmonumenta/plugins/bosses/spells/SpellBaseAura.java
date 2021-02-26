package com.playmonumenta.plugins.bosses.spells;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellBaseAura extends Spell {
	@FunctionalInterface
	public interface ApplyAuraEffect {
		/**
		 * Runs on users when they are within range of the aura
		 * @param player The player to affect
		 */
		void run(Player player);
	}

	private final Entity mBoss;
	private final double mDX;
	private final double mDY;
	private final double mDZ;
	private final double mParticleDX;
	private final double mParticleDY;
	private final double mParticleDZ;
	private final int mNumParticles;
	private final Particle mParticle;
	private final Object mParticleArg;
	private final ApplyAuraEffect mAuraEffect;

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

		// Apply effects every other pulse (2 Hz)
		mEffectIter++;
		if (mEffectIter >= 2 && mAuraEffect != null) {
			mEffectIter = 0;

			for (Player player : PlayerUtils.playersInRange(bossLoc, mRadius)) {
				Location playerLoc = player.getLocation();
				if (Math.abs(playerLoc.getX() - bossLoc.getX()) < mDX &&
				        Math.abs(playerLoc.getX() - bossLoc.getX()) < mDX &&
				        Math.abs(playerLoc.getX() - bossLoc.getX()) < mDX) {
					// Player is within range
					mAuraEffect.run(player);
				}
			}
		}

		// Update the group of nearby players who can see the particles every 5s
		mParticleIter++;
		if (mParticleIter >= 20) {
			mParticleIter = 0;

			// Loop through all nearby players and put the ones that don't have
			// the noAuraParticles tag on a list to send particles to them
			mParticlePlayers.clear();
			for (Player player : PlayerUtils.playersInRange(bossLoc, 80)) {
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

	@Override
	public int cooldownTicks() {
		return 1;
	}
}
