package com.playmonumenta.plugins.bosses.spells;

import java.util.function.Predicate;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;


public class SpellMinionResist extends Spell {
	private final LivingEntity mLauncher;
	private final PotionEffect mPotion;
	private final int mRange;
	private final int mApplyPeriod;
	private final Predicate<Entity> mMinionTester;
	private int mTicks;

	/*
	 * Applies potion effect to launcher whenever other members of his team are within range
	 *
	 * Because this is expected to be called often (passive effect), this only actually does the check
	 * every applyPeriod invocations
	 */
	public SpellMinionResist(LivingEntity launcher, PotionEffect potion, int range, int applyPeriod, Predicate<Entity> minionTester) {
		mLauncher = launcher;
		mPotion = potion;
		mRange = range;
		mApplyPeriod = applyPeriod;
		mMinionTester = minionTester;
		mTicks = mApplyPeriod;
	}

	@Override
	public void run() {
		mTicks++;
		if (mTicks >= mApplyPeriod) {
			mTicks = 0;

			for (Entity e : mLauncher.getNearbyEntities(mRange, mRange, mRange)) {
				if (mMinionTester.test(e)) {
					mLauncher.addPotionEffect(mPotion);
					break;
				}
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}
}
