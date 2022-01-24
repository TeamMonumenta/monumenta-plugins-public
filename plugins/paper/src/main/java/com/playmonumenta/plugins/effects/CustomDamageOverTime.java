package com.playmonumenta.plugins.effects;

import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import javax.annotation.Nullable;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;

public class CustomDamageOverTime extends Effect {

	private final double mDamage;
	private final double mPeriod;
	private final @Nullable Player mPlayer;
	private final @Nullable ClassAbility mSpell;
	private final Particle mParticle;
	private int mTicks;

	public CustomDamageOverTime(int duration, double damage, int period, @Nullable Player player, @Nullable ClassAbility spell, Particle particle) {
		super(duration);
		mDamage = damage;
		mPeriod = period;
		mPlayer = player;
		mSpell = spell;
		mParticle = particle;
	}

	//Magnitude is equal to the level of wither that it is equivalent to, at low levels of wither
	//i.e. a magnitude of 2 means it is the same as wither 2 - deals 1 health per second
	@Override
	public double getMagnitude() {
		return (mDamage * 40.0) / mPeriod;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (fourHertz && entity instanceof LivingEntity le) {
			mTicks += 5; //Activates 4 times a second
			if (mTicks >= mPeriod) {
				mTicks %= mPeriod;
				DamageUtils.damage(mPlayer, le, DamageType.AILMENT, mDamage, mSpell, true, false);
				entity.getWorld().spawnParticle(mParticle, le.getEyeLocation(), 8, 0.4, 0.4, 0.4, 0.1);
			}
		}
	}

	@Override
	public String toString() {
		return String.format("CustomDoT duration:%d modifier:%s damage:%f period:%d", this.getDuration(), "CustomDamageOverTime", mDamage, mPeriod);
	}
}
