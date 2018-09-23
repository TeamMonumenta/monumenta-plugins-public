package com.playmonumenta.bossfights;

import java.util.LinkedList;
import java.util.List;

import com.playmonumenta.bossfights.bosses.BossAbilityGroup;

public class Boss {
	List<BossAbilityGroup> mAbilities;

	public Boss(BossAbilityGroup ability) {
		mAbilities = new LinkedList<BossAbilityGroup>();
		mAbilities.add(ability);
	}

	public void add(BossAbilityGroup ability) {
		mAbilities.add(ability);
	}

	public void unload() {
		/* NOTE
		 *
		 * Unload will cause state to be serialized to the mob's equipment. This is fine if
		 * only one of the BossAbilityGroup's has data to serialize - but if not, only the
		 * last ability will actually have saved data, and the boss will fail to initialize
		 * later.
		 *
		 * Overcoming this limitation requires substantial refactoring.
		 */
		for (BossAbilityGroup ability : mAbilities) {
			ability.unload();
		}
		mAbilities.clear();
	}

	public void death() {
		for (BossAbilityGroup ability : mAbilities) {
			ability.death();
		}
	}
}
