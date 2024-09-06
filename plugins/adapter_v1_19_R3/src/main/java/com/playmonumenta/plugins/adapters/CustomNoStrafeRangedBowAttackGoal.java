package com.playmonumenta.plugins.adapters;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Items;

public class CustomNoStrafeRangedBowAttackGoal extends RangedAttackGoal {
	private final PathfinderMob mMob;
	private final int mAttackIntervalMin;
	private int mAttackTime = -1;
	private int mSeeTime;

	// Copy of the vanilla RangedBowAttackGoal, trimmed down to remove skeleton strafing
	public CustomNoStrafeRangedBowAttackGoal(PathfinderMob actor, double speed, int attackInterval, float range) {
		super((RangedAttackMob) actor, speed, attackInterval, range);
		mMob = actor;
		mAttackIntervalMin = attackInterval;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		return mMob.getTarget() != null && isHoldingBow();
	}

	protected boolean isHoldingBow() {
		return mMob.isHolding(Items.BOW);
	}

	@Override
	public boolean canContinueToUse() {
		return (canUse() || !mMob.getNavigation().isDone()) && isHoldingBow();
	}

	@Override
	public void start() {
		super.start();
		mMob.setAggressive(true);
	}

	@Override
	public void stop() {
		super.stop();
		mMob.setAggressive(false);
		mSeeTime = 0;
		mAttackTime = -1;
		mMob.stopUsingItem();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		LivingEntity livingEntity = mMob.getTarget();
		if (livingEntity != null) {
			boolean bl = mMob.getSensing().hasLineOfSight(livingEntity);
			boolean bl2 = mSeeTime > 0;
			if (bl != bl2) {
				mSeeTime = 0;
			}

			if (bl) {
				++mSeeTime;
			} else {
				--mSeeTime;
			}

			if (mMob.isUsingItem()) {
				if (!bl && mSeeTime < -60) {
					mMob.stopUsingItem();
				} else if (bl) {
					int i = mMob.getTicksUsingItem();
					if (i >= 20) {
						mMob.stopUsingItem();
						((RangedAttackMob) mMob).performRangedAttack(livingEntity, BowItem.getPowerForTime(i));
						mAttackTime = mAttackIntervalMin;
					}
				}
			} else if (--mAttackTime <= 0 && mSeeTime >= -60) {
				mMob.startUsingItem(ProjectileUtil.getWeaponHoldingHand(mMob, Items.BOW));
			}

		}
	}
}
