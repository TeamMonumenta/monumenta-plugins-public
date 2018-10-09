package com.playmonumenta.plugins.abilities;

import com.playmonumenta.plugins.classes.Spells;

import java.util.List;

public class AbilityCollection {

	private List<Ability> mAbilities;

	public AbilityCollection(List<Ability> abilities) {
		mAbilities = abilities;
	}

	public List<Ability> getAbilities() {
		return mAbilities;
	}

	public Ability getAbility(String scoreboardId) {
		for (Ability abil : mAbilities) {
			if (abil.getInfo() != null) {
				AbilityInfo info = abil.getInfo();
				if (info.scoreboardId != null) {
					if (scoreboardId.equals(info.scoreboardId)) {
						return abil;
					}
				}
			}
		}
		return null;
	}

	public Ability getAbility(Spells spell) {
		for (Ability abil : mAbilities) {
			if (abil.getInfo() != null) {
				AbilityInfo info = abil.getInfo();
				if (info.linkedSpell != null) {
					if (spell.equals(info.linkedSpell)) {
						return abil;
					}
				}
			}
		}
		return null;
	}
}
