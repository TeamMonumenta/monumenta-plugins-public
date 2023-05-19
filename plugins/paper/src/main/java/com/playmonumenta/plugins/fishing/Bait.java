package com.playmonumenta.plugins.fishing;

enum Bait {
	GENERAL("Wolfswood Worms", 0.025, 0.05),
	COMBAT("Deep Sea Bait", 0.05, -0.08),
	SPECIALTY("Gourmet Fungus", -0.04, 0.1);

	final String mItemName;
	final double mCombatOdds;
	final double mMinigameOdds;

	Bait(String itemName, double combatOdds, double minigameOdds) {
		this.mItemName = itemName;
		this.mCombatOdds = combatOdds;
		this.mMinigameOdds = minigameOdds;
	}
}
