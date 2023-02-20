package com.playmonumenta.plugins.utils;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.delves.DelvePreset;
import java.util.Arrays;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class DungeonUtils {

	public enum DungeonCommandMapping {
		// King's Valley
		LABS("labs", "D0Access", "D0Finished", "D0StartDate", -1, "D0Type", null),
		WHITE("white", "D1Access", "D1Finished", "D1StartDate", 1, "D1Type", DelvePreset.WHITE),
		ORANGE("orange", "D2Access", "D2Finished", "D2StartDate", 2, "D2Type", DelvePreset.ORANGE),
		MAGENTA("magenta", "D3Access", "D3Finished", "D3StartDate", 3, "D3Type", DelvePreset.MAGENTA),
		LIGHTBLUE("lightblue", "D4Access", "D4Finished", "D4StartDate", 4, "D4Type", DelvePreset.LIGHTBLUE),
		YELLOW("yellow", "D5Access", "D5Finished", "D5StartDate", 5, "D5Type", DelvePreset.YELLOW),
		WILLOWS("willows", "DB1Access", "DB1Finished", "DBWStartDate", 12, "DBWType", DelvePreset.WILLOWS),
		REVERIE("reverie", "DCAccess", "DCFinished", "DMRStartDate", 13, "DMRType", DelvePreset.REVERIE),

		CORRIDORS("corridors", "DRAccess", null, null, -1, null, null),
		SANCTUM("sanctum", "DFSAccess", "DFSFinished", null, -1, null, null),
		VERDANT("verdant", "DVAccess", "DVFinished", null, -1, null, null),
		AZACOR(null, "AzacorAccess", null, null, -1, null, null),

		// Celsian Isles
		LIME("lime", "D6Access", "D6Finished", "D6StartDate", 6, "D6Type", DelvePreset.LIME),
		PINK("pink", "D7Access", "D7Finished", "D7StartDate", 7, "D7Type", DelvePreset.PINK),
		GRAY("gray", "D8Access", "D8Finished", "D8StartDate", 8, "D8Type", DelvePreset.GRAY),
		LIGHTGRAY("lightgray", "D9Access", "D9Finished", "D9StartDate", 9, "D9Type", DelvePreset.LIGHTGRAY),
		CYAN("cyan", "D10Access", "D10Finished", "D10StartDate", 10, "D10Type", DelvePreset.CYAN),
		PURPLE("purple", "D11Access", "D11Finished", "D11StartDate", 11, "D11Type", DelvePreset.PURPLE),
		TEAL("teal", "DTLAccess", "DTLFinished", "DTLStartDate", 14, "DTLType", DelvePreset.TEAL),
		SHIFTINGCITY("shiftingcity", "DRL2Access", "DRL2Finished", "DCSStartDate", 15, "DCSType", DelvePreset.SHIFTING),
		FORUM("forum", "DFFAccess", "DFFFinished", "DFFStartDate", 16, "DFFType", DelvePreset.FORUM),

		MIST("mist", "DBMAccess", "DBMFinished", null, -1, null, null),
		REMORSE("remorse", "DSRAccess", "DSRFinished", null, -1, null, null),
		RUSH("rush", "DRDAccess", "DRDFinished", null, -1, null, null),
		DEPTHS("depths", "DDAccess", "DDFinished", null, -1, null, null),

		// Architect's Ring
		BLUE("blue", "D12Access", "D12Finished", "D12StartDate", 17, null, DelvePreset.BLUE),
		BROWN("brown", "D13Access", "D13Finished", "D13StartDate", 18, null, DelvePreset.BROWN),

		SKT("skt", "DSKTAccess", "DSKTChests", "DSKTStartDate", -1, null, null),
		RUIN("ruin", "DMASAccess", "DMASFinished", null, -1, null, null),
		PORTAL("portal", "DPSAccess", "DPSFinished", null, -1, null, null),
		GALLERY("gallery", "DGAccess", "DGFinished", null, -1, "DGType", null),
		GODSPORE(null, "GodsporeAccess", null, null, -1, null, null);

		private final @Nullable String mShardName;
		private final String mAccessName;
		private final @Nullable String mFinishedName;
		private final @Nullable String mStartDateName;
		private final int mDelveBountyId;
		private final @Nullable String mTypeName;
		private final @Nullable DelvePreset mSavagePreset;

		DungeonCommandMapping(@Nullable String shardName, String accessName, @Nullable String finishedName, @Nullable String startDateName, int delveBountyId, @Nullable String typeName, @Nullable DelvePreset savagePreset) {
			if (delveBountyId > 0 && startDateName == null) {
				throw new IllegalArgumentException("Dungeons with delve bounties must have a start date score");
			}
			mShardName = shardName;
			mAccessName = accessName;
			mFinishedName = finishedName;
			mStartDateName = startDateName;
			mDelveBountyId = delveBountyId;
			mTypeName = typeName;
			mSavagePreset = savagePreset;
		}

		public static final ImmutableMap<String, DungeonCommandMapping> BY_SHARD =
			Arrays.stream(DungeonCommandMapping.values())
				.filter(type -> type.getShardName() != null)
				.collect(ImmutableMap.toImmutableMap(DungeonCommandMapping::getShardName, type -> type));

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

		public int getDelveBountyId() {
			return mDelveBountyId;
		}

		public static @Nullable DungeonCommandMapping getByShard(String name) {
			return BY_SHARD.get(name);
		}

	}

	public static int @Nullable [] getSpawnersBroken(Player p) {
		Location armorStandLoc = p.getWorld().getSpawnLocation(); // get the spawn location
		ArmorStand armorStand = null;
		for (Entity entity : armorStandLoc.getNearbyEntities(2, 2, 2)) { // get the entities at the spawn location
			if (entity.getType().equals(EntityType.ARMOR_STAND) && entity.getCustomName() != null && entity.getCustomName().equals("SpawnerBreaksArmorStand")) { //if it's our marker armorstand
				armorStand = (ArmorStand) entity;
			}
		}
		if (armorStand != null) {
			return new int[] {ScoreboardUtils.getScoreboardValue(armorStand, "SpawnerBreaks").orElse(0), ScoreboardUtils.getScoreboardValue(armorStand, "SpawnersTotal").orElse(0)};
		} else {
			return null;
		}
	}
}

