package com.playmonumenta.plugins.adapters;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class CustomMobAgroMeleeAttack18 extends MeleeAttackGoal {

	private final VersionAdapter.DamageAction mDamageAction;

	public CustomMobAgroMeleeAttack18(PathfinderMob entity, VersionAdapter.DamageAction action) {
		super(entity, 1.0, false);
		mDamageAction = action;
	}


	@Override protected void checkAndPerformAttack(LivingEntity target, double squaredDistance) {
		double d = this.getAttackReachSqr(target);
		if (squaredDistance <= d && this.isTimeToAttack()) {
			this.resetAttackCooldown();
			this.mob.swing(InteractionHand.MAIN_HAND);
			mDamageAction.damage(target.getBukkitLivingEntity());
		}
	}
}
