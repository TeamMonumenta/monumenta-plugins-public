package com.playmonumenta.plugins.fishing;

enum FishLootTable {
	FOREST_FLOUNDER("epic:r3/world/fishing/custom_fishing/fish/forest_flounder", "epic:r3/world/fishing/custom_fishing/fish/forest_flounder_greater"),
	HEXED_SALMON("epic:r3/world/fishing/custom_fishing/fish/hexed_salmon", "epic:r3/world/fishing/custom_fishing/fish/hexed_salmon_greater"),
	KEEP_SIDE_SARDINE("epic:r3/world/fishing/custom_fishing/fish/keep-side_sardine", "epic:r3/world/fishing/custom_fishing/fish/keep-side_sardine_greater"),
	MECHANICAL_MONKFISH("epic:r3/world/fishing/custom_fishing/fish/mechanical_monkfish", "epic:r3/world/fishing/custom_fishing/fish/mechanical_monkfish_greater"),
	MUNGFISH("epic:r3/world/fishing/custom_fishing/fish/mungfish", "epic:r3/world/fishing/custom_fishing/fish/mungfish_greater"),
	SHADE_SEABASS("epic:r3/world/fishing/custom_fishing/fish/shade_seabass", "epic:r3/world/fishing/custom_fishing/fish/shade_seabass_greater"),
	SHROOMFISH("epic:r3/world/fishing/custom_fishing/fish/shroomfish", "epic:r3/world/fishing/custom_fishing/fish/shroomfish_greater"),
	TROUT_OF_THE_ARCHITECT("epic:r3/world/fishing/custom_fishing/fish/trout_of_the_architect", "epic:r3/world/fishing/custom_fishing/fish/trout_of_the_architect_greater"),
	WOLFSWOOD_CARP("epic:r3/world/fishing/custom_fishing/fish/wolfswood_carp", "epic:r3/world/fishing/custom_fishing/fish/wolfswood_carp_greater");

	// The path to the regular loot table, containing tiers 1-5 of the fish.
	final String mPath;
	// The path to the greater loot table, used for minigame rewards, containing only tiers 3-5 of the fish.
	final String mGreaterPath;

	FishLootTable(String path, String greaterPath) {
		mPath = path;
		mGreaterPath = greaterPath;
	}
}
