package com.playmonumenta.plugins.utils;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.delves.DelvePreset;
import com.playmonumenta.plugins.itemstats.enums.Location;
import java.util.Arrays;
import org.jetbrains.annotations.Nullable;

public class DungeonUtils {

	public enum DungeonCommandMapping {
		// King's Valley
		LABS(Location.LABS, "labs", "D0Access", "D0Finished", "D0StartDate", -1, "D0Type", null, "valley"),
		WHITE(Location.WHITE, "white", "D1Access", "D1Finished", "D1StartDate", 1, "D1Type", DelvePreset.WHITE, "valley"),
		ORANGE(Location.ORANGE, "orange", "D2Access", "D2Finished", "D2StartDate", 2, "D2Type", DelvePreset.ORANGE, "valley"),
		MAGENTA(Location.MAGENTA, "magenta", "D3Access", "D3Finished", "D3StartDate", 3, "D3Type", DelvePreset.MAGENTA, "valley"),
		LIGHTBLUE(Location.LIGHTBLUE, "lightblue", "D4Access", "D4Finished", "D4StartDate", 4, "D4Type", DelvePreset.LIGHTBLUE, "valley"),
		YELLOW(Location.YELLOW, "yellow", "D5Access", "D5Finished", "D5StartDate", 5, "D5Type", DelvePreset.YELLOW, "valley"),
		WILLOWS(Location.WILLOWS, "willows", "DB1Access", "DB1Finished", "DBWStartDate", 12, "DBWType", DelvePreset.WILLOWS, "valley"),
		REVERIE(Location.REVERIE, "reverie", "DCAccess", "DCFinished", "DMRStartDate", 13, "DMRType", DelvePreset.REVERIE, "valley"),

		CORRIDORS(Location.EPHEMERAL, "corridors", "DRAccess", null, null, -1, null, null, "valley"),
		SANCTUM(Location.SANCTUM, null, "R1Access", "DFSFinished", null, -1, null, null, "valley"),
		VERDANT(Location.VERDANT, null, "R1Access", "DVFinished", null, -1, null, null, "valley"),
		AZACOR(Location.AZACOR, null, "AzacorAccess", null, null, -1, null, null, "valley"),

		// Celsian Isles
		LIME(Location.LIME, "lime", "D6Access", "D6Finished", "D6StartDate", 6, "D6Type", DelvePreset.LIME, "isles"),
		PINK(Location.PINK, "pink", "D7Access", "D7Finished", "D7StartDate", 7, "D7Type", DelvePreset.PINK, "isles"),
		GRAY(Location.GRAY, "gray", "D8Access", "D8Finished", "D8StartDate", 8, "D8Type", DelvePreset.GRAY, "isles"),
		LIGHTGRAY(Location.LIGHTGRAY, "lightgray", "D9Access", "D9Finished", "D9StartDate", 9, "D9Type", DelvePreset.LIGHTGRAY, "isles"),
		CYAN(Location.CYAN, "cyan", "D10Access", "D10Finished", "D10StartDate", 10, "D10Type", DelvePreset.CYAN, "isles"),
		PURPLE(Location.PURPLE, "purple", "D11Access", "D11Finished", "D11StartDate", 11, "D11Type", DelvePreset.PURPLE, "isles"),
		TEAL(Location.TEAL, "teal", "DTLAccess", "DTLFinished", "DTLStartDate", 14, "DTLType", DelvePreset.TEAL, "isles"),
		SHIFTINGCITY(Location.SHIFTING, "shiftingcity", "DRL2Access", "DRL2Finished", "DCSStartDate", 15, "DCSType", DelvePreset.SHIFTING, "isles"),
		FORUM(Location.FORUM, "forum", "DFFAccess", "DFFFinished", "DFFStartDate", 16, "DFFType", DelvePreset.FORUM, "isles"),

		MIST(Location.MIST, null, "R2Access", "DBMFinished", null, -1, null, null, "isles"),
		REMORSE(Location.REMORSE, null, "R2Access", "DSRFinished", null, -1, null, null, "isles"),
		RUSH(Location.RUSH, "rush", "DRDAccess", "DRDFinished", null, -1, null, null, "isles"),
		DEPTHS(Location.DEPTHS, "depths", "DDAccess", "DDFinished", null, -1, null, null, "isles"),

		// Architect's Ring
		BLUE(Location.BLUE, "blue", "D12Access", "D12Finished", "D12StartDate", 17, null, DelvePreset.BLUE, "ring"),
		BROWN(Location.BROWN, "brown", "D13Access", "D13Finished", "D13StartDate", 18, null, DelvePreset.BROWN, "ring"),

		SKT(Location.SILVER, "skt", "DSKTAccess", "DSKTChests", "DSKTStartDate", -1, null, null, "ring"),
		RUIN(Location.BLUESTRIKE, "ruin", "DMASAccess", "DMASFinished", null, -1, null, null, "ring"),
		PORTAL(Location.SCIENCE, "portal", "DPSAccess", "DPSFinished", null, -1, null, null, "ring"),
		GALLERY(Location.GALLERYOFFEAR, "gallery", "DGAccess", "DGFinished", null, -1, "DGType", null, "ring"),
		GODSPORE(Location.GODSPORE, null, "GodsporeAccess", null, null, -1, null, null, "ring"),
		ZENITH(Location.ZENITH, "zenith", "DCZAccess", "DCZFinished", null, -1, null, null, "ring"),
		HEXFALL(Location.HEXFALL, "hexfall", "DHFAccess", "DHFFinished", "DHFStartDate", -1, null, null, "ring"),
		;

		private final Location mLocation;
		private final @Nullable String mShardName;
		private final String mAccessName;
		private final @Nullable String mFinishedName;
		private final @Nullable String mStartDateName;
		private final int mDelveBountyId;
		private final @Nullable String mTypeName;
		private final @Nullable DelvePreset mSavagePreset;
		/**
		 * The shard to return to if the player is inside the dungeon when it is abandoned
		 */
		private final String mAbandonShardName;

		DungeonCommandMapping(Location location, @Nullable String shardName, String accessName, @Nullable String finishedName, @Nullable String startDateName, int delveBountyId, @Nullable String typeName, @Nullable DelvePreset savagePreset, String returnShardName) {
			if (delveBountyId > 0 && startDateName == null) {
				throw new IllegalArgumentException("Dungeons with delve bounties must have a start date score");
			}
			mLocation = location;
			mShardName = shardName;
			mAccessName = accessName;
			mFinishedName = finishedName;
			mStartDateName = startDateName;
			mDelveBountyId = delveBountyId;
			mTypeName = typeName;
			mSavagePreset = savagePreset;
			mAbandonShardName = returnShardName;
		}

		public static final ImmutableMap<String, DungeonCommandMapping> BY_SHARD =
			Arrays.stream(DungeonCommandMapping.values())
				.filter(type -> type.getShardName() != null)
				.collect(ImmutableMap.toImmutableMap(DungeonCommandMapping::getShardName, type -> type));

		public String getDisplayName() {
			return mLocation.getDisplayName();
		}

		public @Nullable String getShardName() {
			return mShardName;
		}

		public String getAccessName() {
			return mAccessName;
		}

		public @Nullable String getFinishedName() {
			return mFinishedName;
		}

		public @Nullable String getStartDateName() {
			return mStartDateName;
		}

		public @Nullable String getTypeName() {
			return mTypeName;
		}

		public @Nullable DelvePreset getDelvePreset() {
			return mSavagePreset;
		}

		public String getAbandonShardName() {
			return mAbandonShardName;
		}

		public int getDelveBountyId() {
			return mDelveBountyId;
		}

		public static @Nullable DungeonCommandMapping getByShard(String name) {
			return BY_SHARD.get(name);
		}

	}
}
