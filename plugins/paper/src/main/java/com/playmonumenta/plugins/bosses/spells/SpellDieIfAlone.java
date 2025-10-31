package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.bosses.DieIfAloneBoss;
import com.playmonumenta.plugins.utils.DamageUtils;
import java.util.List;
import org.bukkit.entity.LivingEntity;

public class SpellDieIfAlone extends Spell {
	private final LivingEntity mBoss;
	private final DieIfAloneBoss.Parameters mParameters;
	private int mLonelyPulses = 0;

	public SpellDieIfAlone(LivingEntity boss, DieIfAloneBoss.Parameters parameters) {
		mBoss = boss;
		mParameters = parameters;
	}

	@Override
	public void run() {
		if (DieIfAloneBoss.shouldActivate(mBoss, mParameters)) {
			mLonelyPulses++;
			if (mLonelyPulses > mParameters.LONELY_PULSES) {
				DamageUtils.damagePercentHealth(mBoss, mBoss, mParameters.DAMAGE_PERCENTAGE, false, false, "", false, List.of());
			}
		} else {
			mLonelyPulses = 0;
		}
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
