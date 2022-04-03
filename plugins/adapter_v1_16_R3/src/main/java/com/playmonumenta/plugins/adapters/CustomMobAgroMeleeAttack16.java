package com.playmonumenta.plugins.adapters;

import net.minecraft.server.v1_16_R3.EntityCreature;
import net.minecraft.server.v1_16_R3.EntityLiving;
import net.minecraft.server.v1_16_R3.EnumHand;
import net.minecraft.server.v1_16_R3.PathfinderGoalMeleeAttack;

public class CustomMobAgroMeleeAttack16 extends PathfinderGoalMeleeAttack {

	private final VersionAdapter.DamageAction mDamageAction;

	public CustomMobAgroMeleeAttack16(EntityCreature creature, VersionAdapter.DamageAction action) {
		super(creature, 1.0, false);
		mDamageAction = action;
	}


	@Override
	protected void a(EntityLiving target, double distance) {
		double var3 = this.a(target);
		if (distance <= var3 && this.h()) {
			this.g();
			this.a.swingHand(EnumHand.MAIN_HAND);
			mDamageAction.damage(target.getBukkitLivingEntity());
		}

	}
}
