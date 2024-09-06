package com.playmonumenta.plugins.bosses;

import com.playmonumenta.plugins.bosses.spells.Spell;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

/*
This is meant to be an extension of the base SpellManager to allow for bosses to execute a set of attacks
in a specific order, based on their order in the list passed in.
 */
public class SequentialSpellManager extends SpellManager {

	protected Queue<Spell> mReadySequentialSpells;
	private Queue<Spell> mOriginalSpellQueue;

	public SequentialSpellManager(List<Spell> spells) {
		super(spells);
		mOriginalSpellQueue = new ArrayDeque<>(spells);
		mReadySequentialSpells = new ArrayDeque<>(spells);
	}

	@Override
	public int runNextSpell(boolean preventSameSpellTwiceInARow) {
		if (mIsEmpty) {
			return 20;
		}

		if (mReadySequentialSpells.isEmpty()) {
			mReadySequentialSpells = new ArrayDeque<>(mOriginalSpellQueue);
			mCooldownSpells.removeAll(mReadySequentialSpells);
		}

		Queue<Spell> spells = new ArrayDeque<>(mReadySequentialSpells);
		mLastCasted = null;

		Iterator<Spell> iterator = spells.iterator();
		while (iterator.hasNext()) {
			Spell spell = iterator.next();
			if (spell.canRun() && !spell.onlyForceCasted() && !mCooldownSpells.contains(spell)) {
				spell.run();
				mLastCasted = spell;
				mCooldownSpells.add(spell);
				mReadySequentialSpells.remove();
				iterator.remove();
				return spell.cooldownTicks();
			}
		}

		return 20;
	}

	public void clearSpellQueue() {
		mReadySequentialSpells = new ArrayDeque<>();
		mOriginalSpellQueue = new ArrayDeque<>();
	}

	public void addSpellToQueue(Spell spell) {
		mReadySequentialSpells.add(spell);
		mOriginalSpellQueue.add(spell);
	}
}
