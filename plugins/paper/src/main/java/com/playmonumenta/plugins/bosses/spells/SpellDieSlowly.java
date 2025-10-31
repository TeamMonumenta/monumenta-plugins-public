package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.bosses.DieSlowlyBoss;
import com.playmonumenta.plugins.utils.DamageUtils;
import java.util.List;
import org.bukkit.entity.LivingEntity;

public class SpellDieSlowly extends Spell {
	private final LivingEntity mBoss;
	private final DieSlowlyBoss.Parameters mParameters;

	public SpellDieSlowly(LivingEntity boss, DieSlowlyBoss.Parameters parameters) {
		mBoss = boss;
		mParameters = parameters;
	}

	@Override
	public void run() {
		DamageUtils.damagePercentHealth(mBoss, mBoss, mParameters.DAMAGE_PERCENTAGE, false, false, " ", false, List.of());
	}

	@Override
	public int cooldownTicks() {
		return mParameters.TICKS_PER_PULSE;
	}

	@Override
	public boolean bypassSilence() {
		return true;
	}
}
