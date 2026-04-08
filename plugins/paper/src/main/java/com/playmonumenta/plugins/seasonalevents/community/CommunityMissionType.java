package com.playmonumenta.plugins.seasonalevents.community;

import com.playmonumenta.plugins.seasonalevents.MonumentaContent;
import java.util.Arrays;
import java.util.List;

public enum CommunityMissionType {
	// goal tiers can probably change once i get more data on what is fair

	// world bosses
	KAUL(Category.WORLD_BOSS, "Kaul", "Defeat Kaul's", MonumentaContent.KAUL, 50, 100, 200, 1, 5),
	ELDRASK(Category.WORLD_BOSS, "Eldrask", "Defeat Eldrask's", MonumentaContent.ELDRASK, 50, 100, 200, 1, 5),
	HEKAWT(Category.WORLD_BOSS, "Hekawt", "Defeat Hekawt's", MonumentaContent.HEKAWT, 50, 100, 200, 1, 5),
	SIRIUS(Category.WORLD_BOSS, "Sirius", "Defeat Sirius'", MonumentaContent.SIRIUS, 50, 100, 200, 1, 5),

	// other bosses
	AZACOR(Category.BOSS, "Azacor", "Defeat Azacors", MonumentaContent.AZACOR, 200, 500, 1000, 3, 10),
	HORSEMAN(Category.BOSS, "The Horseman", "Defeat Headless Horsemans", MonumentaContent.HORSEMAN, 200, 500, 1000, 3, 10),
	SNOW_SPIRIT(Category.BOSS, "Snow Spirit", "Defeat Snow Spirits", MonumentaContent.SNOW_SPIRIT, 200, 500, 1000, 3, 10),
	INTRUDER(Category.BOSS, "The Intruder", "Defeat Intruders", MonumentaContent.INTRUDER, 200, 500, 1000, 3, 10),
	GODSPORE(Category.BOSS, "Godspore", "Defeat Godspores", MonumentaContent.GODSPORE, 200, 500, 1000, 3, 10),
	RUTEN(Category.BOSS, "Ruten", "Defeat Ru'tens", MonumentaContent.HEXFALL_RUTEN, 200, 500, 1000, 3, 10),

	// strikes
	SANCTUM(Category.STRIKE, "Sanctum", "Complete Forsworn Sanctum's", MonumentaContent.SANCTUM, 300, 600, 1200, 5, 15),
	VERDANT(Category.STRIKE, "Verdant", "Complete Verdant Remnant's", MonumentaContent.VERDANT, 300, 600, 1200, 5, 15),
	MIST(Category.STRIKE, "Mist", "Complete Black Mist's", MonumentaContent.MIST, 300, 600, 1200, 5, 15),
	REMORSE(Category.STRIKE, "Remorse", "Complete Sealed Remorse's", MonumentaContent.REMORSE, 300, 600, 1200, 5, 15),
	RUIN(Category.STRIKE, "Ruin", "Complete Masquerader's Ruins", MonumentaContent.RUIN, 300, 600, 1200, 5, 15),
	PORTAL(Category.STRIKE, "Portal", "Complete Portals", MonumentaContent.PORTAL, 300, 600, 1200, 5, 15),

	// endless content
	CORRIDORS(Category.ENDLESS, "Corridors", "Clear Corridors Rooms", MonumentaContent.CORRIDORS_ROOM, 15000, 30000, 60000, 300, 750),
	RUSH(Category.ENDLESS, "Rush", "Complete Rush Waves", MonumentaContent.RUSH, 1000, 2500, 5000, 20, 50),
	DEPTHS(Category.ENDLESS, "Depths", "Clear Depths Rooms", MonumentaContent.DEPTHS, 15000, 30000, 60000, 300, 1000),
	ZENITH(Category.ENDLESS, "Zenith", "Clear Zenith Ascension Levels", MonumentaContent.ZENITH, 2500, 5000, 10000, 50, 150),
	GALLERY(Category.ENDLESS, "Gallery", "Complete Gallery Waves", MonumentaContent.GALLERY, 500, 1000, 2000, 10, 25),

	// poi biomes
	STARPOINT(Category.POI, "Starpoint", "Complete Starpoint POI's", MonumentaContent.STARPOINT_POI, 100, 250, 500, 1, 5),
	KEEP(Category.POI, "Keep", "Complete Keep POI's", MonumentaContent.KEEP_POI, 100, 250, 500, 1, 5),
	WOLFSWOOD(Category.POI, "Wolfswood", "Complete Wolfswood POI's", MonumentaContent.WOLFSWOOD_POI, 100, 250, 500, 1, 5),

	// misc (should probably add total spawners here later too)
	HUNTS_UNSPOILED(Category.MISC, "Hunts Unspoiled", "Complete Hunts Unspoiled", MonumentaContent.HUNTS_UNSPOILED, 100, 250, 500, 1, 5),
	FISHING_COMBAT(Category.MISC, "Fishing Combats", "Complete Fishing Combats", MonumentaContent.FISHING_COMBAT, 100, 250, 500, 1, 5);

	public enum Category {
		WORLD_BOSS, BOSS, STRIKE, ENDLESS, POI, MISC
	}

	public final Category mCategory;
	public final String mName;
	public final String mDescription;
	public final MonumentaContent mContent;

	public final int mGoalTier1;
	public final int mGoalTier2;
	public final int mGoalTier3;
	public final int mContribTier1;
	public final int mContribTier2;

	CommunityMissionType(Category category, String name, String description, MonumentaContent content,
						 int goalTier1, int goalTier2, int goalTier3, int contribTier1, int contribTier2) {
		this.mCategory = category;
		this.mName = name;
		this.mDescription = description;
		this.mContent = content;
		this.mGoalTier1 = goalTier1;
		this.mGoalTier2 = goalTier2;
		this.mGoalTier3 = goalTier3;
		this.mContribTier1 = contribTier1;
		this.mContribTier2 = contribTier2;
	}

	public static List<CommunityMissionType> getAll() {
		return Arrays.asList(values());
	}
}
