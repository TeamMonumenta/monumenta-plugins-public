package com.playmonumenta.plugins.adapters;

import net.minecraft.server.v1_16_R3.EntityCreature;
import net.minecraft.server.v1_16_R3.EntityLiving;
import net.minecraft.server.v1_16_R3.PathfinderGoalMeleeAttack;

public class CustomPathfinderGoalMeleeAttack16 extends PathfinderGoalMeleeAttack {

	private final double mAttackRange;
	private final double mAttackHeight;

	public CustomPathfinderGoalMeleeAttack16(EntityCreature entity, double speed, boolean pauseWhenMobIdle, double attackRange, double attackHeight) {
		super(entity, speed, pauseWhenMobIdle);
		mAttackRange = attackRange;
		mAttackHeight = attackHeight;
	}

	@Override
	protected double a(EntityLiving target) {
		// to make it possible for entities to not attack from their feet,
		// calculate whether the attack can hit here and return positive or negative infinity to allow/disallow the attack.
		double x = a.locX();
		double y = a.locY() + mAttackHeight;
		double z = a.locZ();
		if (target.h(x, y, z) <= mAttackRange * mAttackRange) {
			return Double.POSITIVE_INFINITY;
		} else {
			return Double.NEGATIVE_INFINITY;
		}
	}

}
