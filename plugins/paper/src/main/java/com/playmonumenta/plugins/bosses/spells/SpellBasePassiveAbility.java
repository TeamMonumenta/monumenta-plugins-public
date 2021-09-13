package com.playmonumenta.plugins.bosses.spells;

public class SpellBasePassiveAbility extends Spell {

	private int mCooldown;
	private Spell mSpell;
	private int mT = 0;

	public SpellBasePassiveAbility(int cooldown, Spell spell) {
		mCooldown = cooldown;
		mSpell = spell;
	}

	@Override
	public void run() {
		mT += 5;
		if (mT >= mCooldown) {
			mT = 0;
			mSpell.run();
		}
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}

}
