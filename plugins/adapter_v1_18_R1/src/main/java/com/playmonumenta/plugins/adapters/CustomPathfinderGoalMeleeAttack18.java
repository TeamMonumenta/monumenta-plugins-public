package com.playmonumenta.plugins.adapters;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class CustomPathfinderGoalMeleeAttack18 extends MeleeAttackGoal {

	private final double mAttackRange;
	private final double mAttackHeight;

	public CustomPathfinderGoalMeleeAttack18(PathfinderMob entity, double speed, boolean pauseWhenMobIdle, double attackRange, double attackHeight) {
		super(entity, speed, pauseWhenMobIdle);
		mAttackRange = attackRange;
		mAttackHeight = attackHeight;
	}

	@Override
	protected double getAttackReachSqr(LivingEntity target) {
		double x = mob.getX();
		double y = mob.getY() + mAttackHeight;
		double z = mob.getZ();
		if (target.distanceToSqr(x, y, z) <= mAttackRange * mAttackRange) {
			return Double.POSITIVE_INFINITY;
		} else {
			return Double.NEGATIVE_INFINITY;
		}
	}

}
