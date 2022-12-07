package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

public class SpellRemoveLevitation extends Spell {

	private final LivingEntity mBoss;

	public SpellRemoveLevitation(LivingEntity boss) {
		mBoss = boss;
	}

	@Override
	public void run() {
		if (mBoss.isValid() && !mBoss.isDead()) {
			mBoss.removePotionEffect(PotionEffectType.LEVITATION);
		}
	}

	@Override
	public boolean bypassSilence() {
		return true;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
