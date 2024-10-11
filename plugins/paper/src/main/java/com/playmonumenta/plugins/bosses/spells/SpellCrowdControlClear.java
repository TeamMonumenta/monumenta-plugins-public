package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
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
		if (EntityUtils.isSlowed(Plugin.getInstance(), mBoss)) {
			EntityUtils.setSlowTicks(Plugin.getInstance(), mBoss, 0);
		}
		if (EntityUtils.isBleeding(Plugin.getInstance(), mBoss)) {
			EntityUtils.setBleedTicks(Plugin.getInstance(), mBoss, 0);
		}
		mBoss.removePotionEffect(PotionEffectType.LEVITATION);
	}

	@Override
	public int cooldownTicks() {
		return mTimer;
	}
}
