package com.playmonumenta.plugins.bosses.spells.lich.undeadplayers;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.lich.SpellDimensionDoor;

public class SpellLichKeyGlow extends Spell {
	LivingEntity mBoss;

	public SpellLichKeyGlow(LivingEntity boss) {
		mBoss = boss;
	}

	@Override
	public void run() {
		if (SpellDimensionDoor.getShadowed().size() > 0) {
			mBoss.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 0, false));
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
