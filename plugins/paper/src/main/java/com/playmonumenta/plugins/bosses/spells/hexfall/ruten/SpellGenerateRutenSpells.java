package com.playmonumenta.plugins.bosses.spells.hexfall.ruten;

import com.playmonumenta.plugins.bosses.bosses.hexfall.Ruten;
import com.playmonumenta.plugins.bosses.spells.Spell;

public class SpellGenerateRutenSpells extends Spell {

	private final Ruten mBossInstance;

	public SpellGenerateRutenSpells(Ruten bossInstance) {
		mBossInstance = bossInstance;
	}

	@Override
	public void run() {
		mBossInstance.generateSequentialActiveSpells();
	}

	@Override
	public int cooldownTicks() {
		return 20 * 2;
	}
}
