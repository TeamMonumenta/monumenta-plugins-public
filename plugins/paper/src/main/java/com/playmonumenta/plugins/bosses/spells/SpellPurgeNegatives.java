package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

public class SpellPurgeNegatives extends Spell {
	private int mCooldown = 0;
	private LivingEntity mBoss;
	private int mTimer;

	private static final PotionEffectType[] NEGATIVE_EFFECTS = new PotionEffectType[] {
		PotionEffectType.BLINDNESS,
		PotionEffectType.POISON,
		PotionEffectType.CONFUSION,
		PotionEffectType.SLOW,
		PotionEffectType.SLOW_DIGGING,
		PotionEffectType.WITHER,
		PotionEffectType.WEAKNESS,
		PotionEffectType.HARM,
		PotionEffectType.HUNGER,
		PotionEffectType.LEVITATION,
		PotionEffectType.UNLUCK
	};
	public SpellPurgeNegatives(LivingEntity boss, int timer) {
		mBoss = boss;
		mTimer = timer;
	}

	@Override
	public void run() {
		mCooldown -= 5;
		if (mCooldown <= 0) {
			mCooldown = mTimer;
			for (PotionEffectType type : NEGATIVE_EFFECTS) {
				mBoss.removePotionEffect(type);
			}
		}
	}

	@Override
	public int duration() {
		// TODO Auto-generated method stub
		return 0;
	}

}
