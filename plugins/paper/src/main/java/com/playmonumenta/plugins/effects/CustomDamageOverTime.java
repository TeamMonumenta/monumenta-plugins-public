package com.playmonumenta.plugins.effects;

import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;

public class CustomDamageOverTime extends Effect {

	private final double mDamage;
	private final double mPeriod;
	private final Plugin mPlugin;
	private final Player mPlayer;
	private final ClassAbility mSpell;
	private final MagicType mMagic;
	private final Particle mParticle;

	private int mTicks;

	public CustomDamageOverTime(int duration, double damage, int period, Player player, MagicType magic, ClassAbility spell, Particle particle, Plugin plugin) {
		super(duration);
		mDamage = damage;
		mPeriod = period;
		mPlayer = player;
		mSpell = spell;
		mMagic = magic;
		mParticle = particle;
		mPlugin = plugin;
	}

	//Magnitude is equal to the level of wither that it is equivalent to, at low levels of wither
	//i.e. a magnitude of 2 means it is the same as wither 2 - deals 1 health per second
	@Override
	public double getMagnitude() {
		return (mDamage * 40.0) / mPeriod;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (entity instanceof LivingEntity && fourHertz) {
			mTicks += 5; //Activates 4 times a second
			LivingEntity le = (LivingEntity) entity;
			if (mTicks >= mPeriod) {
				mTicks %= mPeriod;
				EntityUtils.damageEntity(mPlugin, le, mDamage, mPlayer, mMagic, true, mSpell, false, false, true, true);
				mPlayer.getWorld().spawnParticle(mParticle, le.getEyeLocation(), 8, 0.4, 0.4, 0.4, 0.1);
			}
		}
	}

	@Override
	public String toString() {
		return String.format("CustomDoT duration:%d modifier:%s damage:%f period:%d", this.getDuration(), "CustomDamageOverTime", mDamage, mPeriod);
	}
}
