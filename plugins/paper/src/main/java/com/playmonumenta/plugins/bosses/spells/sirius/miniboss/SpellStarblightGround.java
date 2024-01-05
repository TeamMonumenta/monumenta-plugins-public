package com.playmonumenta.plugins.bosses.spells.sirius.miniboss;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.sirius.PassiveStarBlightConversion;
import org.bukkit.entity.LivingEntity;

public class SpellStarblightGround extends Spell {

	private PassiveStarBlightConversion mConverter;
	private LivingEntity mBoss;
	private static final int RADIUS = 4;

	public SpellStarblightGround(LivingEntity boss, PassiveStarBlightConversion converter) {
		mConverter = converter;
		mBoss = boss;
	}

	@Override
	public void run() {
		if (mBoss.isOnGround()) {
			mConverter.convertSphere(RADIUS, mBoss.getLocation());
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
