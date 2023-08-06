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

	public CustomMobAgroMeleeAttack18(PathfinderMob entity, VersionAdapter.DamageAction action) {
		this(entity, action, false);
	}

	public CustomMobAgroMeleeAttack18(PathfinderMob entity, VersionAdapter.DamageAction action, boolean requireSight) {
		super(entity, 1.0, false);
		mDamageAction = action;
		mRequireSight = requireSight;
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

}
