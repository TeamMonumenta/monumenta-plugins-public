package com.playmonumenta.plugins.poi;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

public enum POI {
	// TODO: Implement Display Color, to be used in item displays
	AMANITA_COLONY("amanita_colony", "Amanita Colony", "epic:r3/world/poi/amanita_colony/endweekly", "#4C8F4D", "wolfswood"),
	ARX_SPIRENSIS("arx_spirensis", "Arx Spirensis", "epic:r3/world/poi/arx_spirensis/endweekly", "#C4BBA5", "keep"),
	BEWITCHED_DOMINION("bewitched_dominion", "Bewitched Dominion", "epic:r3/world/poi/farm/endweekly", "#4C8F4D", "wolfswood"),
	CELESTIAL_RAMPART("celestial_rampart", "Celestial Rampart", "epic:r3/world/poi/celestial_rampart/endweekly", "#342768", "starpoint"),
	CHANTERELLE_VILLAGE("chanterelle_village", "Chanterelle Village", "epic:r3/world/poi/chanterelle_village/endweekly", "#4C8F4D", "wolfswood"),
	CHITTERING_GUTTERS("chittering_gutters", "Chittering Gutters", "epic:r3/world/poi/chittering_gutters/endweekly", "#342768", "starpoint"),
	CONSTELLATION_TOWER("constellation_tower", "Constellation Tower", "epic:r3/world/poi/constellation_tower/endweekly", "#342768", "starpoint"),
	COVEN_FORTRESS("coven_fortress", "Coven Fortress", "epic:r3/world/poi/coven_fortress/endweekly", "#4C8F4D", "wolfswood"),
	DOOMED_ENCAMPMENT("doomed_encampment", "Doomed Encampment", "epic:r3/world/poi/doomed_encampment/endweekly", "#C4BBA5", "keep"),
	FORSAKEN_MANOR("forsaken_manor", "Forsaken Manor", "epic:r3/world/poi/forsaken_manor/endweekly", "#C4BBA5", "keep"),
	LOCUM_VERNANTIA("locum_vernantia", "Locum Vernantia", "epic:r3/world/poi/locum_vernatia/endweekly", "#4C8F4D", "wolfswood"),
	QUELLED_CONVENT("quelled_convent", "Quelled Convent", "epic:r3/world/poi/cathedral/endweekly", "#C4BBA5", "keep"),
	SHADOWCAST_BASTILLE("shadowcast_bastille", "Shadowcast Bastille", "epic:r3/world/poi/silverstrike_bastille/endweekly", "#4C8F4D", "wolfswood"),
	SILVIC_QUARRY("silvic_quarry", "Silvic Quarry", "epic:r3/world/poi/terracotta_mine/endweekly", "#C4BBA5", "keep"),
	STARBOUND_SANCTUARY("starbound_sanctuary", "Starbound Sanctuary", "epic:r3/world/poi/starbound_sanctuary/endweekly", "#342768", "starpoint"),
	SUBMERGED_CITADEL("submerged_citadel", "Submerged Citadel", "epic:r3/world/poi/waterfall_village/endweekly", "#C4BBA5", "keep"),
	THE_NADIR("the_nadir", "The Nadir", "epic:r3/world/poi/the_nadir/endweekly", "#342768", "starpoint"),
	THE_TOLUMAEUS("the_tolumaeus", "The Tolumaeus", "epic:r3/world/poi/the_tolumaeus/endweekly", "#342768", "starpoint"),
	VIBRANT_HOLLOW("vibrant_hollow", "Vibrant Hollow", "epic:r3/world/poi/vibrant_hollow/endweekly", "#4C8F4D", "wolfswood"),
	NONE("none", "None", "temp_path", "", "none");

	public static final Map<String, POI> REVERSE_MAPPINGS = Arrays.stream(POI.values())
		.collect(Collectors.toUnmodifiableMap(POI::getName, type -> type));

	final String mName;
	final String mCleanName;
	final String mLootPath;
	final String mDisplayColor;
	final String mLocation;

	POI(String name, String cleanName, String lootPath, String displayColor, String location) {
		mName = name;
		mCleanName = cleanName;
		mLootPath = lootPath;
		mDisplayColor = displayColor;
		mLocation = location;
	}

	public String getName() {
		return mName;
	}

	public String getCleanName() {
		return mCleanName;
	}

	public String getLootPath() {
		return mLootPath;
	}

	public String getDisplayColor() {
		return mDisplayColor;
	}

	public String getLocation() {
		return mLocation;
	}

	public static @Nullable POI getPOI(String name) {
		return REVERSE_MAPPINGS.get(name);
	}

}
