package com.playmonumenta.plugins.fishing;

import org.jetbrains.annotations.Nullable;

enum Bait {
	GENERAL("Wolfswood Worms", 0.025, 0.05, 0.1, null, 0),
	COMBAT("Deep Sea Bait", 0.05, -0.1, 0, null, 0),
	SPECIALTY("Gourmet Fungus", -0.05, 0.1, 0, null, 0),
	QUALITY("Appetizing Crickets", 0, 0, 0.4, null, 0),

	// Fish-Specific bait
	HALLOWED_SNAIL("Hallowed Snail", 0, 0, 0.2, FishLootTable.TROUT_OF_THE_ARCHITECT, 0.5),
	FADED_REMNANTS("Faded Remnants", 0, 0, 0.2, FishLootTable.SHADE_SEABASS, 0.5),
	HEXCRAFTED_FLESH("Hexcrafted Flesh", 0, 0, 0.2, FishLootTable.HEXED_SALMON, 0.5),
	ZOOPLANKTON("Zooplankton", 0, 0, 0.2, FishLootTable.KEEP_SIDE_SARDINE, 0.5),
	DELICIOUS_ALGAE("Delicious Algae", 0, 0, 0.2, FishLootTable.WOLFSWOOD_CARP, 0.5),
	SOUL_GRUBS("Soul Grubs", 0, 0, 0.2, FishLootTable.MECHANICAL_MONKFISH, 0.5),
	TINY_SHELLFISH("Tiny Shellfish", 0, 0, 0.2, FishLootTable.FOREST_FLOUNDER, 0.5),
	FRUIT_MEAL("Odorous Fruit Meal", 0, 0, 0.2, FishLootTable.MUNGFISH, 0.5),
	BLUE_CRYSTAL("Smoky Blue Crystal", 0, 0, 0.2, FishLootTable.SHROOMFISH, 0.5);

	final String mItemName;
	final double mCombatOdds;
	final double mMinigameOdds;
	final double mQualityIncreaseOdds;
	final @Nullable FishLootTable mReplacementLootTable;
	final double mReplacementChance;

	Bait(String itemName, double combatOdds, double minigameOdds, double qualityOdds, @Nullable FishLootTable replacementLootTable, double replacementChance) {
		mItemName = itemName;
		mCombatOdds = combatOdds;
		mMinigameOdds = minigameOdds;
		mQualityIncreaseOdds = qualityOdds;
		mReplacementLootTable = replacementLootTable;
		mReplacementChance = replacementChance;
	}
}
