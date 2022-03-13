package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.utils.EntityUtils;
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
		double maxHealth = EntityUtils.getAttributeOrDefault(mLauncher, Attribute.GENERIC_MAX_HEALTH, 0);
		double health = mLauncher.getHealth();
		if (mLauncher.isValid() && !mLauncher.isDead() && health <= maxHealth / 2) {
			mLauncher.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20, 0, false, true));
		}
	}

	@Override
	public int cooldownTicks() {
		return 20;
	}
}
