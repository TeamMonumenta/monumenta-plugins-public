package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

public class CrowdControlImmunity extends Spell {

	private LivingEntity mBoss;

	public CrowdControlImmunity(LivingEntity boss) {
		this.mBoss = boss;
	}

	@Override
	public void run() {
		mBoss.removePotionEffect(PotionEffectType.SLOW);
		mBoss.removePotionEffect(PotionEffectType.LEVITATION);
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}

}
