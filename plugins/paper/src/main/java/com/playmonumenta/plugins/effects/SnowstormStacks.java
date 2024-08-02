package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.abilities.frostborn.Snowstorm;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;


public class SnowstormStacks extends Effect {
	public static final String effectID = "SnowstormStacks";

	private int mStacks;
	private final Player mPlayer;
	private final double mSlowAmount;
	private final int mSlowDuration;
	private final int mFreezeStacks;
	private final double mFreezeDamage;
	private final int mFreezeDuration;
	private boolean mHasFrozenYet;

	public SnowstormStacks(Player player, int stacks, double slowAmount, int slowDuration, int freezeStacks, double freezeDamage, int freezeDuration) {
		super(slowDuration, effectID);
		mPlayer = player;
		mStacks = stacks;
		mSlowAmount = slowAmount;
		mSlowDuration = slowDuration;
		mFreezeStacks = freezeStacks;
		mFreezeDamage = freezeDamage;
		mFreezeDuration = freezeDuration;

		mHasFrozenYet = false;
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity instanceof LivingEntity mob) {
			incrementStacks(mob);
		}
	}

	public void incrementStacks(LivingEntity mob) {
		setDuration(Snowstorm.SLOW_DURATION);
		mStacks++;

		double slowness = Math.min(5, mStacks) * mSlowAmount;
		EntityUtils.applySlow(Plugin.getInstance(), mSlowDuration, slowness, mob);

		if (mStacks >= mFreezeStacks && !mHasFrozenYet) {
			mHasFrozenYet = true;
			DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, mFreezeDamage, ClassAbility.SNOWSTORM, true, false);
			if (!EntityUtils.isBoss(mob) && !EntityUtils.isCCImmuneMob(mob)) {
				EntityUtils.applyFreeze(Plugin.getInstance(), mFreezeDuration, mob);
			}
		}
	}

	@Override
	public double getMagnitude() {
		return mStacks;
	}

	@Override
	public String toString() {
		return String.format(
			"%s | duration:%s stacks:%s",
			this.getClass().getName(),
			getDuration(),
			getMagnitude()
		);
	}
}
