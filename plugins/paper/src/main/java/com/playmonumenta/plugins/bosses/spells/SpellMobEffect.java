package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;

public class SpellMobEffect extends Spell {
	private LivingEntity mLauncher;
	private PotionEffect mPotion;

	public SpellMobEffect(LivingEntity launcher, PotionEffect potion) {
		mLauncher = launcher;
		mPotion = potion;
	}

	@Override
	public void run() {
		mLauncher.addPotionEffect(mPotion, true);
	}

	@Override
	public int duration() {
		return 1;
	}
}
