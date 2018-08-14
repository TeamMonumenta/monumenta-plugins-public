package com.playmonumenta.bossfights.spells;

import com.playmonumenta.bossfights.utils.Utils;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.material.MaterialData;
import org.bukkit.Particle;

public class SpellBaseAura implements Spell
{
	@FunctionalInterface
	public interface ApplyAuraEffect
	{
		/**
		 * Runs on users when they are within range of the aura
		 * @param player The player to affect
		 */
		void run(Player player);
	}

	private Entity mBoss;
	private double mDX;
	private double mDY;
	private double mDZ;
	private double mParticleDX;
	private double mParticleDY;
	private double mParticleDZ;
	private int mNumParticles;
	private Particle mParticle;
	private MaterialData mColor;
	private ApplyAuraEffect mAuraEffect;

	private int mRadius; // Computed maximum of mDX, mDY, mDZ
	private int mIter; // Number of iterations - rolls around between 1 and 4

	public SpellBaseAura(Entity boss, double dx, double dy, double dz, int numParticles,
	                     Particle particle, MaterialData color, ApplyAuraEffect auraEffect)
	{
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
		mColor = color;
		mAuraEffect = auraEffect;

		mRadius = (int)Math.max(mDX, Math.max(mDY, mDZ));
		mIter = 0;
	}

	/*
	 * This spell is designed to be run passively (4x a second).
	 * Effects don't need to be applied to players that frequently -
	 * so only particles run every invocation, effects only one in four
	 */
	@Override
	public void run()
	{
		Location bossLoc = mBoss.getLocation();

		// Generate particles in area
		bossLoc.getWorld().spawnParticle(mParticle, bossLoc, mNumParticles, mParticleDX, mParticleDY, mParticleDZ, mColor);
		// Generate particles immediately around boss
		bossLoc.getWorld().spawnParticle(mParticle, bossLoc.clone().add(0, 1, 0), 5, 1, 1, 1, mColor);

		// Apply effects every four iterations
		mIter++;
		if (mIter >= 4 && mAuraEffect != null)
		{
			mIter = 0;

			for (Player player : Utils.playersInRange(bossLoc, mRadius))
			{
				Location playerLoc = player.getLocation();
				if (Math.abs(playerLoc.getX() - bossLoc.getX()) < mDX &&
				    Math.abs(playerLoc.getX() - bossLoc.getX()) < mDX &&
				    Math.abs(playerLoc.getX() - bossLoc.getX()) < mDX)
				{
					// Player is within range
					mAuraEffect.run(player);
				}
			}
		}
	}
}
