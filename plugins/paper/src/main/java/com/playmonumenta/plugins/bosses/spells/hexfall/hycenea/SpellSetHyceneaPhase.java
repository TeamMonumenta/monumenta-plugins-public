package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.bosses.bosses.hexfall.HyceneaRageOfTheWolf;
import com.playmonumenta.plugins.bosses.spells.Spell;

public class SpellSetHyceneaPhase extends Spell {
	private final HyceneaRageOfTheWolf mHycenea;
	private final int mPhase;
	private final int mCooldown;

	public SpellSetHyceneaPhase(HyceneaRageOfTheWolf hycenea, int phase, int cooldown) {
		mHycenea = hycenea;
		mPhase = phase;
		mCooldown = cooldown;
	}

	@Override
	public void run() {
		mHycenea.setPhase(mPhase);
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
