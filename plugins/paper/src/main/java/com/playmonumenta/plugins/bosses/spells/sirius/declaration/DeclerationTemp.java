package com.playmonumenta.plugins.bosses.spells.sirius.declaration;

import com.playmonumenta.plugins.bosses.bosses.sirius.Sirius;
import com.playmonumenta.plugins.bosses.spells.Spell;

public class DeclerationTemp extends Spell {
	private Sirius mSirius;

	public DeclerationTemp(Sirius sirius) {
		mSirius = sirius;
	}

	@Override
	public void run() {
		mSirius.changeHp(false, 19);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
