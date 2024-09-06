package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.bosses.bosses.hexfall.HyceneaRageOfTheWolf;
import com.playmonumenta.plugins.bosses.spells.Spell;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class SpellAllowTotemThrow extends Spell {

	private final LivingEntity mBoss;
	private final boolean mAllowThrow;

	public SpellAllowTotemThrow(LivingEntity boss, boolean allowThrow) {
		mBoss = boss;
		mAllowThrow = allowThrow;
	}

	@Override
	public void run() {
		for (Entity entity : mBoss.getNearbyEntities(HyceneaRageOfTheWolf.detectionRange, HyceneaRageOfTheWolf.detectionRange, HyceneaRageOfTheWolf.detectionRange)) {
			if (entity.getScoreboardTags().contains("Hycenea_Center")) {
				if (mAllowThrow) {
					entity.removeScoreboardTag("Hycenea_Totem_NoThrow");
				} else {
					entity.addScoreboardTag("Hycenea_Totem_NoThrow");
				}
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
