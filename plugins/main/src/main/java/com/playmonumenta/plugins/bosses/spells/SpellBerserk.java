package com.playmonumenta.bossfights.spells;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpellBerserk extends Spell {
	private final LivingEntity mLauncher;

	public SpellBerserk(LivingEntity boss) {
		mLauncher = boss;
	}

	@Override
	public void run() {
		double maxHealth = mLauncher.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
		double health = mLauncher.getHealth();
		if (health <= maxHealth / 2) {
			mLauncher.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20, 0, false, true));
		}
	}

	@Override
	public int duration() {
		return 20;
	}
}
