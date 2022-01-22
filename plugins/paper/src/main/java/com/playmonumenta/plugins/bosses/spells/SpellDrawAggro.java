package com.playmonumenta.plugins.bosses.spells;

import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import com.playmonumenta.plugins.utils.EntityUtils;

public class SpellDrawAggro extends Spell {
	private LivingEntity mBoss;
	private double mRadius;

	public SpellDrawAggro(LivingEntity boss, double radius) {
		mBoss = boss;
		mRadius = radius;
	}

	@Override
	public void run() {
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mBoss.getLocation(), mRadius);
		for (LivingEntity le : mobs) {
			if (le instanceof Mob mob && !EntityUtils.isBoss(mob)) {
				mob.setTarget(mBoss);
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
