package com.playmonumenta.plugins.adapters;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.util.RayTraceResult;

public class CustomMobAgroMeleeAttack18 extends MeleeAttackGoal {

	private final VersionAdapter.DamageAction mDamageAction;
	private final boolean mRequireSight;
	private final double mAttackRangeSquared;

	public CustomMobAgroMeleeAttack18(PathfinderMob entity, VersionAdapter.DamageAction action) {
		this(entity, action, false);
	}

	public CustomMobAgroMeleeAttack18(PathfinderMob entity, VersionAdapter.DamageAction action, double attackRange) {
		this (entity, action, false, attackRange);
	}

	public CustomMobAgroMeleeAttack18(PathfinderMob entity, VersionAdapter.DamageAction action, boolean requireSight) {
		this(entity, action, requireSight, 0);
	}

	public CustomMobAgroMeleeAttack18(PathfinderMob entity, VersionAdapter.DamageAction action, boolean requireSight, double attackRange) {
		super(entity, 1.0, false);
		mDamageAction = action;
		mRequireSight = requireSight;
		mAttackRangeSquared = attackRange * attackRange;
	}


	@Override
	protected void checkAndPerformAttack(LivingEntity target, double squaredDistance) {
		double d = this.getAttackReachSqr(target);
		if (squaredDistance <= d && this.isTimeToAttack() && (!mRequireSight || hasLineOfSight(this.mob.getBukkitLivingEntity(), target.getBukkitLivingEntity(), Math.sqrt(squaredDistance)))) {
			this.resetAttackCooldown();
			this.mob.swing(InteractionHand.MAIN_HAND);
			mDamageAction.damage(target.getBukkitLivingEntity());
		}
	}

	private boolean hasLineOfSight(org.bukkit.entity.LivingEntity attacker, org.bukkit.entity.LivingEntity target, double dist) {
		Location aLoc = attacker.getEyeLocation();
		Location tLoc = target.getLocation();
		RayTraceResult result = aLoc.getWorld().rayTrace(aLoc, tLoc.toVector().subtract(aLoc.toVector()).normalize(), dist, FluidCollisionMode.NEVER, true, 0, e -> e == target);
		return result != null && result.getHitEntity() == target;
	}

	@Override
	//Horribly Scuffed
	protected double getAttackReachSqr(LivingEntity target) {
		if (mAttackRangeSquared == 0) {
			return (double)(this.mob.getBbWidth() * 2.0F * this.mob.getBbWidth() * 2.0F + target.getBbWidth());
		}

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
