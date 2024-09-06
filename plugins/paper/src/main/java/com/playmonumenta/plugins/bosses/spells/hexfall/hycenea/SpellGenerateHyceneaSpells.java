package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.bosses.bosses.hexfall.HyceneaRageOfTheWolf;
import com.playmonumenta.plugins.bosses.spells.Spell;

public class SpellGenerateHyceneaSpells extends Spell {
	private final HyceneaRageOfTheWolf mBossInstance;

	public SpellGenerateHyceneaSpells(HyceneaRageOfTheWolf bossInstance) {
		mBossInstance = bossInstance;
	}

	@Override
	public void run() {
		mBossInstance.addSpellsToQueue();
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
