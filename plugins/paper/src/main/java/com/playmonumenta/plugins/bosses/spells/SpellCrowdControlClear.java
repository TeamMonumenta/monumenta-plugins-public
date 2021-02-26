package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

public class SpellCrowdControlClear extends Spell {
	//Super simple spell to clear crowd control for variable times
	private LivingEntity mBoss;
	private int mTimer;

	public SpellCrowdControlClear(LivingEntity boss, int timer) {
		mBoss = boss;
		mTimer = timer;
	}

	@Override
	public void run() {
		//To make sure abilities can actually freeze the boss
		if (mBoss.getPotionEffect(PotionEffectType.SLOW) != null && mBoss.getPotionEffect(PotionEffectType.SLOW).getAmplifier() < 15) {
			mBoss.removePotionEffect(PotionEffectType.SLOW);
		}
		mBoss.removePotionEffect(PotionEffectType.LEVITATION);
	}

	@Override
	public int cooldownTicks() {
		return mTimer;
	}
}
