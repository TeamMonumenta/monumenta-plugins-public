package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.utils.EntityUtils;

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
			if (EntityUtils.isSlowed(com.playmonumenta.plugins.Plugin.getInstance(), mBoss)) {
				EntityUtils.setSlowTicks(com.playmonumenta.plugins.Plugin.getInstance(), mBoss, 0);
			}
			if (EntityUtils.isBleeding(com.playmonumenta.plugins.Plugin.getInstance(), mBoss)) {
				EntityUtils.setBleedTicks(com.playmonumenta.plugins.Plugin.getInstance(), mBoss, 0);
			}
		}
	}

	@Override
	public int cooldownTicks() {
		// TODO Auto-generated method stub
		return 0;
	}

}
