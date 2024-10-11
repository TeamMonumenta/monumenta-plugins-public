package com.playmonumenta.plugins.seasonalevents;

import org.jetbrains.annotations.Nullable;

public enum MissionType {

	// Clear a specific piece of content x times (examples: blue, eldrask, rush)
	CONTENT("content"),
	// Clear x amount of dungeons (all)
	DUNGEONS("dungeons"),
	// Clear x amount of strikes (all)
	STRIKES("strikes"),
	// Clear x amount of bosses (all)
	BOSSES("bosses"),
	// Break x amount of spawners
	SPAWNERS("spawners"),
	SPAWNERS_POI("spawners_poi"),
	// Clear any content with x modifier
	DELVE_MODIFIER("delve_modifier"), // Also checks content array
	DELVE_POINTS("delve_points"), // Also checks content array
	CHALLENGE_DELVE("challenge_delve"), // Also checks content array
	// Clear x waves in rod (cumulative)
	ROD_WAVES("rod_waves"),
	// Clear x rooms in depths (cumulative)
	DEPTHS_ROOMS("depths_rooms"),
	ZENITH_ROOMS("zenith_rooms"),
	ZENITH_ASCENSION("zenith_ascension"),
	DAILY_BOUNTY("daily_bounty"),
	DELVE_BOUNTY("delve_bounty"),
	// comes from a specific region
	REGIONAL_CONTENT("regional_content"),
	POI_BIOME("poi_biome");

	private final String mType;

	MissionType(String type) {
		mType = type;
	}

	public String getType() {
		return mType;
	}

	public static @Nullable MissionType getMissionTypeSelection(String type) {
		if (type == null) {
			return null;
		}
		for (MissionType selection : values()) {
			if (selection.getType().equals(type)) {
				return selection;
			}
		}
		return null;
	}
}
