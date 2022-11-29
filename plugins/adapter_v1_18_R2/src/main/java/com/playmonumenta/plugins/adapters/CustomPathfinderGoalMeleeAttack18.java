package com.playmonumenta.plugins.adapters;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * Melee attack goal that has a customizable attack range, and also calculates range between two mobs' centers instead of between their feet.
 */
public class CustomPathfinderGoalMeleeAttack18 extends MeleeAttackGoal {

	private final double mAttackRangeSquared;

	public CustomPathfinderGoalMeleeAttack18(PathfinderMob entity, double speed, boolean pauseWhenMobIdle, double attackRange) {
		super(entity, speed, pauseWhenMobIdle);
		mAttackRangeSquared = attackRange * attackRange;
	}

	@Override
	protected double getAttackReachSqr(LivingEntity target) {
		double dx = mob.getX() - target.getX();
		double dy = mob.getY() + mob.getBbHeight() / 2 - (target.getY() + target.getBbHeight() / 2);
		double dz = mob.getZ() - target.getZ();
		if (dx * dx + dy * dy + dz * dz <= mAttackRangeSquared) {
			return Double.POSITIVE_INFINITY;
		} else {
			return Double.NEGATIVE_INFINITY;
		}
	}

}
