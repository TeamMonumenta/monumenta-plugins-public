package com.playmonumenta.bossfights;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.playmonumenta.bossfights.spells.Spell;

/*
 * The SpellManager class is designed to manage active spells for a boss. It
 * provides a simple interface to run a random spell from the provided list of
 * available spells. It automatically checks each spell's canRun() and moves to
 * the next spell seamlessly if conditions are not met.
 *
 * The SpellManager also automatically manages spell cooldowns to make the
 * fight a little less repetitive. The number of steps in between running the
 * same spell again is calculated as floor((#spells - 1) / 2). So if there are
 * 1 or 2 spells there is no cooldown and either spell is equally likely. 3-4
 * spells is a cooldown of 1, meaning a spell can never be chosen again the
 * very next time a spell is invoked. 5-6 spells is a cooldown of 2 (can not be
 * chosen either immediately afterward OR the time after that). Etc.
 */
public class SpellManager {
	List<Spell> mReadySpells;
	Queue<Spell> mCooldownSpells;
	int mCooldown;

	public SpellManager(List<Spell> spells) {
		/*
		 * Need a new copy of the list because the passed-in version doesn't
		 * support removing during iteration... Weird
		 */
		mReadySpells = new ArrayList<Spell>();
		for (Spell spell : spells) {
			mReadySpells.add(spell);
		}

		mCooldownSpells = new LinkedList<Spell>();
		mCooldown = (int)Math.floor(((double)mReadySpells.size() - 1.0) / 2.0);
	}

	public int runNextSpell() {
		/*
		 * If a spell has been on cooldown sufficiently long, remove it from
		 * the cooldown list and add it to the ready list.
		 */
		if (mCooldownSpells.size() > mCooldown) {
			mReadySpells.add(mCooldownSpells.remove());
		}


		/*
		 * Try the ready spells in random order until can be run or none remain
		 */
		Collections.shuffle(mReadySpells);
		Iterator<Spell> iterator = mReadySpells.iterator();
		while (iterator.hasNext()) {
			Spell spell = iterator.next();
			if (spell.canRun()) {
				spell.run();
				mCooldownSpells.add(spell);
				iterator.remove();

				/* Return how much time the spell takes */
				return spell.duration();
			}
		}

		/* None of these spells can run - wait a second before trying again */
		return 20;
	}
}

