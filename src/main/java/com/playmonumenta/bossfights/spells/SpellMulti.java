package com.playmonumenta.bossfights.spells;

import java.util.ArrayList;
import java.util.List;


public class SpellMulti implements Spell {
	private List<Spell> mSpells;

	public SpellMulti(Spell ... spells) {
		mSpells = new ArrayList<Spell>(spells.length);
		for (Spell spell : spells) {
			mSpells.add(spell);
		}
	}

	/*
	 * Can only run if all sub-spells can run
	 */
	@Override
	public boolean canRun() {
		for (Spell spell : mSpells) {
			if (!spell.canRun()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void run() {
		for (Spell spell : mSpells) {
			spell.run();
		}
	}

	/*
	 * Duration is that of longest spell
	 */
	@Override
	public int duration() {
		int longestDuration = 1;

		for (Spell spell : mSpells) {
			if (longestDuration < spell.duration()) {
				longestDuration = spell.duration();
			}
		}

		return longestDuration;
	}
}
