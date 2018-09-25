package com.playmonumenta.plugins.specializations;

import com.playmonumenta.plugins.Plugin.Classes;

public enum ClassSpecialization {

	NONE(Classes.NONE, 0),
	SWORDSAGE(Classes.ROGUE, 1),
	ASSASSIN(Classes.ROGUE, 2),
	PYROMANCER(Classes.MAGE, 3),
	CYROMANCER(Classes.MAGE, 4),
	REAPER(Classes.WARLOCK, 5),
	TENEBRIST(Classes.WARLOCK, 6),
	SNIPER(Classes.SCOUT, 7),
	RANGER(Classes.SCOUT, 8),
	PALADIN(Classes.CLERIC, 9),
	HIEROPHANT(Classes.CLERIC, 10),
	BERSERKER(Classes.WARRIOR, 11),
	ELEMENTALIST(Classes.MAGE, 12),
	ARCANIST(Classes.MAGE, 13);

	private final Classes linkedClass;
	private final int id;

	ClassSpecialization(Classes linkedClass, int id) {
		this.linkedClass = linkedClass;
		this.id = id;
	}

	public Classes getLinkedClass() {
		return linkedClass;
	}

	public int getId() {
		return id;
	}

}
