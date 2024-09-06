package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.bosses.bosses.hexfall.HarrakfarGodOfLife;
import com.playmonumenta.plugins.bosses.spells.Spell;

public class SpellGenerateBlueSpells extends Spell {
	private final HarrakfarGodOfLife mBossInstance;

	public SpellGenerateBlueSpells(HarrakfarGodOfLife bossInstance) {
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
