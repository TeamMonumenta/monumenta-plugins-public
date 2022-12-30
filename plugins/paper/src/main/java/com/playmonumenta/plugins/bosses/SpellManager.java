package com.playmonumenta.plugins.bosses;

import com.playmonumenta.plugins.bosses.spells.Spell;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import org.jetbrains.annotations.Nullable;

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
	public static final SpellManager EMPTY = new SpellManager(Collections.emptyList());

	private Map<Class<? extends Spell>, Spell> mReadySpells;
	private final Queue<Spell> mCooldownSpells;
	private final int mCooldown;
	private final boolean mIsEmpty;
	private @Nullable Spell mLastCasted = null;

	public boolean isEmpty() {
		return mIsEmpty;
	}

	public List<Spell> getSpells() {
		List<Spell> spells = new ArrayList<Spell>();
		if (mIsEmpty) {
			return spells;
		}

		spells.addAll(mCooldownSpells);
		spells.addAll(mReadySpells.values());
		return spells;
	}

	public SpellManager(List<Spell> spells) {
		mIsEmpty = spells.isEmpty();
		if (mIsEmpty) {
			mReadySpells = Collections.emptyMap();
		} else {
			mReadySpells = new HashMap<Class<? extends Spell>, Spell>();
			for (Spell spell : spells) {
				mReadySpells.put(spell.getClass(), spell);
			}
		}

		mCooldownSpells = new ArrayDeque<Spell>();
		mCooldown = (int)Math.max(0, Math.floor((mReadySpells.size() - 1.0) / 2.0));
	}

	public int runNextSpell() {
		/* Standard 1s delay with no spells */
		if (mIsEmpty) {
			return 20;
		}

		/*
		 * If a spell has been on cooldown sufficiently long, remove it from
		 * the cooldown list and add it to the ready list.
		 */
		if (mCooldownSpells.size() > mCooldown) {
			Spell toAdd = mCooldownSpells.remove();
			mReadySpells.put(toAdd.getClass(), toAdd);
		}

		/* No active spells, exit early */
		if (mReadySpells.isEmpty()) {
			return 20;
		}

		/*
		 * Try the ready spells in random order until can be run or none remain
		 */
		List<Spell> spells = new ArrayList<Spell>(mReadySpells.values());
		Collections.shuffle(spells);
		mLastCasted = null;
		Iterator<Spell> iterator = spells.iterator();
		while (iterator.hasNext()) {
			Spell spell = iterator.next();
			if (spell.canRun() && !spell.onlyForceCasted()) {
				spell.run();
				mLastCasted = spell;
				mCooldownSpells.add(spell);
				iterator.remove();
				/* Return how much time the spell takes */
				return spell.cooldownTicks();
			}
		}

		/* None of these spells can run - wait a second before trying again */
		return 20;
	}

	public int forceCastSpell(Class<? extends Spell> spell) {
		/* Standard 1s delay with no spells */
		if (mIsEmpty) {
			return 20;
		}

		if (mLastCasted != null) {
			mLastCasted.cancel();
			mLastCasted = null;
		}
		Spell sp = mReadySpells.get(spell);
		if (sp != null && sp.canRun()) {
			sp.run();
			mLastCasted = sp;
			mCooldownSpells.add(sp);
			return sp.cooldownTicks();
		}
		/* None of these spells can run - wait a second before trying again */
		return 20;
	}

	public @Nullable Spell getLastCastedSpell() {
		return mLastCasted;
	}

	public void cancelAll() {
		if (!mIsEmpty) {
			for (Spell spell : mReadySpells.values()) {
				spell.cancel();
			}
			for (Spell spell : mCooldownSpells) {
				spell.cancel();
			}
		}
	}
}
