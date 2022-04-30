package com.playmonumenta.plugins.seasonalevents;

import org.checkerframework.checker.nullness.qual.Nullable;

public enum WeeklyMissionType {

	//Content- Clear a specific piece of content x times (examples- blue, eldrask, rush)
	//Dungeons- Clear x amount of dungeons (all)
	//Strikes- Clear x amount of strikes (all)
	//Bosses- Clear x amount of bosses (all)
	//Spawners- Break x amount of spawners
	//Delves- Clear any content with x modifier
	//Rod Waves- Clear x waves in rod (cumulative)
	//Depths Rooms- Clear x rooms in depths (cumulative)
	//Regional- comes from a specific region

	CONTENT("content"),
	DUNGEONS("dungeons"),
	STRIKES("strikes"),
	BOSSES("bosses"),
	SPAWNERS("spawners"),
	SPAWNERS_POI("spawners_poi"),
	DELVE_MODIFIER("delve_modifier"), //Also checks content array
	DELVE_POINTS("delve_points"), //Also checks content array
	ROD_WAVES("rod_waves"),
	DEPTHS_ROOMS("depths_rooms"),
	DAILY_BOUNTY("daily_bounty"),
	DELVE_BOUNTY("delve_bounty"),
	REGIONAL_CONTENT("regional_content");

	private final String mType;

	WeeklyMissionType(String type) {
		mType = type;
	}

	public String getType() {
		return mType;
	}

	public static @Nullable WeeklyMissionType getMissionTypeSelection(String type) {
		if (type == null) {
			return null;
		}
		for (WeeklyMissionType selection : WeeklyMissionType.values()) {
			if (selection.getType().equals(type)) {
				return selection;
			}
		}
		return null;
	}
}
