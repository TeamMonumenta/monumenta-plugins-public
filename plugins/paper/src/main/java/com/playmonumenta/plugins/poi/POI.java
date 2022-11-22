package com.playmonumenta.plugins.poi;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

public enum POI {
	// TODO: Implement Display Color, to be used in item displays
	AMANITA_COLONY("amanita_colony", "Amanita Colony", "epic:r3/world/poi/amanita_colony/endweekly", "#4C8F4D"),
	ARX_SPIRENSIS("arx_spirensis", "Arx Spirensis", "epic:r3/world/poi/arx_spirensis/endweekly", "#C4BBA5"),
	BEWITCHED_DOMINION("bewitched_dominion", "Bewitched Dominion", "epic:r3/world/poi/farm/endweekly", "#4C8F4D"),
	CHANTERELLE_VILLAGE("chanterelle_village", "Chanterelle Village", "epic:r3/world/poi/chanterelle_village/endweekly", "#4C8F4D"),
	COVEN_FORTRESS("coven_fortress", "Coven Fortress", "epic:r3/world/poi/coven_fortress/endweekly", "#4C8F4D"),
	DOOMED_ENCAMPMENT("doomed_encampment", "Doomed Encampment", "epic:r3/world/poi/doomed_encampment/endweekly", "#C4BBA5"),
	FORSAKEN_MANOR("forsaken_manor", "Forsaken Manor", "epic:r3/world/poi/forsaken_manor/endweekly", "#C4BBA5"),
	LOCUM_VERNANTIA("locum_vernantia", "Locum Vernantia", "epic:r3/world/poi/locum_vernatia/endweekly", "#4C8F4D"),
	QUELLED_CONVENT("quelled_convent", "Quelled Convent", "epic:r3/world/poi/cathedral/endweekly", "#C4BBA5"),
	SHADOWCAST_BASTILLE("shadowcast_bastille", "Shadowcast Bastille", "epic:r3/world/poi/silverstrike_bastille/endweekly", "#4C8F4D"),
	SILVIC_QUARRY("silvic_quarry", "Silvic Quarry", "epic:r3/world/poi/terracotta_mine/endweekly", "#C4BBA5"),
	SUBMERGED_CITADEL("submerged_citadel", "Submerged Citadel", "epic:r3/world/poi/waterfall_village/endweekly", "#C4BBA5"),
	VIBRANT_HOLLOW("vibrant_hollow", "Vibrant Hollow", "epic:r3/world/poi/vibrant_hollow/endweekly", "#4C8F4D"),
	NONE("none", "None", "temp_path", "");

	public static final Map<String, POI> REVERSE_MAPPINGS = Arrays.stream(POI.values())
		.collect(Collectors.toUnmodifiableMap(type -> type.getName(), type -> type));

	final String mName;
	final String mCleanName;
	final String mLootPath;
	final String mDisplayColor;

	POI(String name, String cleanName, String lootPath, String displayColor) {
		mName = name;
		mCleanName = cleanName;
		mLootPath = lootPath;
		mDisplayColor = displayColor;
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

	public static @Nullable POI getPOI(String name) {
		return REVERSE_MAPPINGS.get(name);
	}

}
