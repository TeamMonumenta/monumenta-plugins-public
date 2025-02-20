
package com.playmonumenta.plugins.adapters.v1_20_R3;

import com.playmonumenta.mixinapi.v1.CustomMeleeAttackGoal;
import com.playmonumenta.plugins.adapters.VersionAdapter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftLivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomMobAgroMeleeAttack extends MeleeAttackGoal implements CustomMeleeAttackGoal {
	@FunctionalInterface
	public interface RangePredicate {
		boolean test(PathfinderMob mob, LivingEntity target, double attackRangeSqr);
	}

	public static class Builder {
		private final PathfinderMob mMob;
		private VersionAdapter.DamageAction mAction = null;
		private boolean mRequireSight = false;
		private RangePredicate mRangeChecker = (mob, target, attackRangeSqr) -> {
			double dx = mob.getX() - target.getX();
			double dy = mob.getY() + mob.getBbHeight() / 2 - (target.getY() + target.getBbHeight() / 2);
			double dz = mob.getZ() - target.getZ();

			return dx * dx + dy * dy + dz * dz <= attackRangeSqr;
		};
		private double mAttackRange = 0;
		private double mSpeed = 1;
		private boolean mPauseWhenMobIdle = false;

		public Builder(PathfinderMob entity) {
			this.mMob = entity;
		}

		public Builder action(VersionAdapter.DamageAction action) {
			this.mAction = action;
			return this;
		}

		public Builder requireSight(boolean requireSight) {
			this.mRequireSight = requireSight;
			return this;
		}

		public Builder attackRange(double attackRange) {
			this.mAttackRange = attackRange;
			return this;
		}

		public Builder speed(double speed) {
			this.mSpeed = speed;
			return this;
		}

		public Builder pauseWhenMobIdle(boolean pauseWhenMobIdle) {
			this.mPauseWhenMobIdle = pauseWhenMobIdle;
			return this;
		}

		public Builder rangeChecker(RangePredicate rangeChecker) {
			this.mRangeChecker = rangeChecker;
			return this;
		}

		public CustomMobAgroMeleeAttack build() {
			return new CustomMobAgroMeleeAttack(mMob, mAction, mRequireSight, mAttackRange, mSpeed,
				mPauseWhenMobIdle,
				mRangeChecker);
		}
	}

	public static Builder builder(PathfinderMob mob) {
		return new Builder(mob);
	}

	@Nullable
	private final VersionAdapter.DamageAction mDamageAction;
	private final boolean mRequireSight;
	private final double mAttackRangeSquared;
	private final RangePredicate mRangeChecker;

	private CustomMobAgroMeleeAttack(
		PathfinderMob entity,
		@Nullable VersionAdapter.DamageAction action,
		boolean requireSight,
		double attackRange,
		double speed,
		boolean pauseWhenMobIdle,
		RangePredicate rangeChecker
	) {
		super(entity, speed, pauseWhenMobIdle);
		mDamageAction = action;
		this.mRequireSight = requireSight;
		mAttackRangeSquared = attackRange * attackRange;
		this.mRangeChecker = rangeChecker;
	}

	@Override
	protected void checkAndPerformAttack(@NotNull LivingEntity target) {
		if (this.isTimeToAttack() && isWithinAttackRange(target.getBukkitLivingEntity()) && (!mRequireSight || this.mob.getSensing().hasLineOfSight(target))) {
			this.resetAttackCooldown();
			this.mob.swing(InteractionHand.MAIN_HAND);
			if (mDamageAction == null) {
				this.mob.doHurtTarget(target);
			} else {
				mDamageAction.damage(target.getBukkitLivingEntity());
			}
		}
	}

	// The naming scheme here is a bit terrible, sorry :(
	@Override
	public boolean isWithinAttackRange(org.bukkit.entity.LivingEntity e) {
		final var target = ((CraftLivingEntity) e).getHandle();

		if (mAttackRangeSquared == 0) {
			return this.mob.isWithinMeleeAttackRange(target);
		}

		return mRangeChecker.test(mob, target, mAttackRangeSquared);
	}
}
