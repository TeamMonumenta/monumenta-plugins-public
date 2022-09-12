package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import javax.annotation.Nullable;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class CustomDamageOverTime extends Effect {
	public static final String effectID = "CustomDamageOverTime";

	protected final double mDamage;
	protected final int mPeriod;
	protected final @Nullable Player mPlayer;
	protected final @Nullable ClassAbility mSpell;
	protected final Particle mParticle;
	private int mTicks;

	public CustomDamageOverTime(int duration, double damage, int period, @Nullable Player player, @Nullable ClassAbility spell, Particle particle) {
		super(duration, effectID);
		mDamage = damage;
		mPeriod = period;
		mPlayer = player;
		mSpell = spell;
		mParticle = particle;
	}

	// Dummy constructor for copying
	public CustomDamageOverTime() {
		this(0, 0, 1, null, null, null);
	}

	//Magnitude is equal to the level of wither that it is equivalent to, at low levels of wither
	//i.e. a magnitude of 2 means it is the same as wither 2 - deals 1 health per second
	@Override
	public double getMagnitude() {
		return (mDamage * 40.0) / mPeriod;
	}

	@Override
	public boolean isDebuff() {
		return true;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (fourHertz && entity instanceof LivingEntity le && !(entity instanceof ArmorStand)) {
			mTicks += 5; //Activates 4 times a second
			if (mTicks >= mPeriod) {
				mTicks %= mPeriod;
				DamageUtils.damage(mPlayer, le, DamageType.AILMENT, mDamage, mSpell, true, false);
				new PartialParticle(mParticle, le.getEyeLocation(), 8, 0.4, 0.4, 0.4, 0.1).spawnAsEnemy();
			}
		}
	}

	@Override
	public String toString() {
		return String.format("CustomDoT duration:%d modifier:%s damage:%f period:%d", this.getDuration(), "CustomDamageOverTime", mDamage, mPeriod);
	}
}
