package com.playmonumenta.plugins.utils;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.commands.ShardSorterCommand;
import com.playmonumenta.plugins.delves.DelvePreset;
import com.playmonumenta.plugins.delves.DelvesManager;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.itemstats.enums.Location;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.worlds.paper.MonumentaWorldManagementAPI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public enum DungeonCommandMapping {
	// King's Valley
	LABS(Location.LABS, "labs", false, true, "D0Access", "D0Finished", "D0StartDate", 21, "D0LastVisit", -1, "D0Type", null, null, "valley"),
	WHITE(Location.WHITE, "white", false, false, "D1Access", "D1Finished", "D1StartDate", 21, "D1LastVisit", 1, "D1Type", 1, DelvePreset.WHITE, "valley"),
	ORANGE(Location.ORANGE, "orange", false, false, "D2Access", "D2Finished", "D2StartDate", 21, "D2LastVisit", 2, "D2Type", 1, DelvePreset.ORANGE, "valley"),
	MAGENTA(Location.MAGENTA, "magenta", false, false, "D3Access", "D3Finished", "D3StartDate", 21, "D3LastVisit", 3, "D3Type", 1, DelvePreset.MAGENTA, "valley"),
	LIGHTBLUE(Location.LIGHTBLUE, "lightblue", false, false, "D4Access", "D4Finished", "D4StartDate", 21, "D4LastVisit", 4, "D4Type", 1, DelvePreset.LIGHTBLUE, "valley"),
	YELLOW(Location.YELLOW, "yellow", false, false, "D5Access", "D5Finished", "D5StartDate", 21, "D5LastVisit", 5, "D5Type", 1, DelvePreset.YELLOW, "valley"),
	WILLOWS(Location.WILLOWS, "willows", false, false, "DB1Access", "DB1Finished", "DBWStartDate", 21, "DBWLastVisit", 12, "DBWType", 1, DelvePreset.WILLOWS, "valley"),
	REVERIE(Location.REVERIE, "reverie", false, false, "DCAccess", "DCFinished", "DMRStartDate", 21, "DMRLastVisit", 13, "DMRType", 1, DelvePreset.REVERIE, "valley"),

	CORRIDORS(Location.EPHEMERAL, "corridors", false, false, "DRAccess", null, null, null, null, -1, null, null, null, "valley"),
	SANCTUM(Location.SANCTUM, null, false, false, "R1Access", "DFSFinished", null, null, null, -1, null, null, null, "valley"),
	VERDANT(Location.VERDANT, null, false, false, "R1Access", "DVFinished", null, null, null, -1, null, null, null, "valley"),
	AZACOR(Location.AZACOR, null, false, false, "AzacorAccess", null, null, null, null, -1, null, null, null, "valley"),

	// Celsian Isles
	LIME(Location.LIME, "lime", false, false, "D6Access", "D6Finished", "D6StartDate", 21, "D6LastVisit", 6, "D6Type", 1, DelvePreset.LIME, "isles"),
	PINK(Location.PINK, "pink", false, false, "D7Access", "D7Finished", "D7StartDate", 21, "D7LastVisit", 7, "D7Type", 1, DelvePreset.PINK, "isles"),
	GRAY(Location.GRAY, "gray", false, false, "D8Access", "D8Finished", "D8StartDate", 21, "D8LastVisit", 8, "D8Type", 1, DelvePreset.GRAY, "isles"),
	LIGHTGRAY(Location.LIGHTGRAY, "lightgray", false, false, "D9Access", "D9Finished", "D9StartDate", 21, "D9LastVisit", 9, "D9Type", 1, DelvePreset.LIGHTGRAY, "isles"),
	CYAN(Location.CYAN, "cyan", false, false, "D10Access", "D10Finished", "D10StartDate", 21, "D10LastVisit", 10, "D10Type", 1, DelvePreset.CYAN, "isles"),
	PURPLE(Location.PURPLE, "purple", false, false, "D11Access", "D11Finished", "D11StartDate", 21, "D11LastVisit", 11, "D11Type", 1, DelvePreset.PURPLE, "isles"),
	TEAL(Location.TEAL, "teal", false, false, "DTLAccess", "DTLFinished", "DTLStartDate", 21, "DTLLastVisit", 14, "DTLType", 1, DelvePreset.TEAL, "isles"),
	SHIFTINGCITY(Location.SHIFTING, "shiftingcity", false, false, "DRL2Access", "DRL2Finished", "DCSStartDate", 21, "DCSLastVisit", 15, "DCSType", 1, DelvePreset.SHIFTING, "isles"),
	FORUM(Location.FORUM, "forum", false, false, "DFFAccess", "DFFFinished", "DFFStartDate", 21, "DFFLastVisit", 16, "DFFType", null, DelvePreset.FORUM, "isles"),

	MIST(Location.MIST, null, false, false, "R2Access", "DBMFinished", null, null, null, -1, null, null, null, "isles"),
	REMORSE(Location.REMORSE, null, false, false, "R2Access", "DSRFinished", null, null, null, -1, null, null, null, "isles"),
	RUSH(Location.RUSH, "rush", false, false, "DRDAccess", "DRDFinished", null, null, null, -1, null, null, null, "isles"),
	DEPTHS(Location.DEPTHS, "depths", false, false, "DDAccess", "DDFinished", null, null, null, -1, null, null, null, "isles"),

	// Architect's Ring
	BLUE(Location.BLUE, "blue", false, false, "D12Access", "D12Finished", "D12StartDate", 21, "D12LastVisit", 17, null, null, DelvePreset.BLUE, "ring"),
	BROWN(Location.BROWN, "brown", false, false, "D13Access", "D13Finished", "D13StartDate", 21, "D13LastVisit", 18, null, null, DelvePreset.BROWN, "ring"),
	INDIGO(Location.INDIGO, "indigo", false, false, "DIAccess", "DIFinished", "DIStartDate", 21, "DILastVisit", 19, null, null, DelvePreset.INDIGO, "ring"),

	SKT(Location.SILVER, "skt", false, true, "DSKTAccess", "DSKTChests", "DSKTStartDate", 7, "DSKTLastVisit", -1, null, null, null, "SPECIAL_CASE_SKT"),
	RUIN(Location.BLUESTRIKE, "ruin", false, false, "DMASAccess", "DMASFinished", null, null, null, -1, null, null, null, "ring"),
	PORTAL(Location.SCIENCE, "portal", false, false, "DPSAccess", "DPSFinished", null, null, null, -1, null, null, null, "ring"),
	GALLERY(Location.GALLERYOFFEAR, "gallery", false, false, "DGAccess", "DGFinished", null, null, null, -1, "DGType", null, null, "ring"),
	GODSPORE(Location.GODSPORE, null, false, false, "GodsporeAccess", null, null, null, null, -1, null, null, null, "ring"),
	ZENITH(Location.ZENITH, "zenith", false, false, "DCZAccess", "DCZFinished", null, null, null, -1, null, null, null, "ring"),
	HEXFALL(Location.HEXFALL, "hexfall", true, true, "DHFAccess", "DHFFinished", "DHFStartDate", 21, "DHFLastVisit", -1, null, null, null, "ring"),
	;

	private final Location mLocation;
	private final @Nullable String mShardName;
	private final boolean mCanAlwaysInvite;
	private final boolean mCanAlwaysAbandon;
	private final String mAccessName;
	private final @Nullable String mFinishedName;
	private final @Nullable String mStartDateName;
	private final @Nullable Integer mDurationDays;
	private final @Nullable String mLastVisitName;
	private final int mDelveBountyId;
	private final @Nullable String mTypeName;
	private final @Nullable Integer mExaltedTypeScore;
	private final @Nullable DelvePreset mSavagePreset;
	/**
	 * The shard to return to if the player is inside the dungeon when it is abandoned
	 */
	private final String mAbandonShardName;

	DungeonCommandMapping(
		Location location,
		@Nullable String shardName,
		boolean canAlwaysInvite,
		boolean canAlwaysAbandon,
		String accessName,
		@Nullable String finishedName,
		@Nullable String startDateName,
		@Nullable Integer durationDays,
		@Nullable String lastVisitName,
		int delveBountyId,
		@Nullable String typeName,
		@Nullable Integer exaltedTypeScore,
		@Nullable DelvePreset savagePreset,
		String returnShardName
	) {
		if (delveBountyId > 0 && startDateName == null) {
			throw new IllegalArgumentException("Dungeons with delve bounties must have a start date score");
		}
		mLocation = location;
		mShardName = shardName;
		mCanAlwaysInvite = canAlwaysInvite;
		mCanAlwaysAbandon = canAlwaysAbandon;
		mAccessName = accessName;
		mFinishedName = finishedName;
		mStartDateName = startDateName;
		mDurationDays = durationDays;
		mLastVisitName = lastVisitName;
		mDelveBountyId = delveBountyId;
		mTypeName = typeName;
		mExaltedTypeScore = exaltedTypeScore;
		mSavagePreset = savagePreset;
		mAbandonShardName = returnShardName;
	}

	public static final ImmutableMap<String, DungeonCommandMapping> BY_SHARD =
		Arrays.stream(values())
			.filter(type -> type.getShardName() != null)
			.collect(ImmutableMap.toImmutableMap(DungeonCommandMapping::getShardName, type -> type));

	public String getLocationName() {
		return mLocation.getName();
	}

	public String getDisplayName() {
		return mLocation.getDisplayName();
	}

	public @Nullable String getShardName() {
		return mShardName;
	}

	public boolean isChallenge(Player player) {
		return mShardName != null && DelvesManager.validateDelvePreset(player, mShardName);
	}

	public boolean isExalted(Player player) {
		if (mTypeName == null || mExaltedTypeScore == null) {
			return false;
		}
		return ScoreboardUtils.getScoreboardValue(player, mTypeName).orElse(0) == mExaltedTypeScore;
	}

	public boolean canAlwaysInvite() {
		return mCanAlwaysInvite;
	}

	public @Nullable Integer getLastInviteDate(Player player) {
		if (mCanAlwaysInvite) {
			Integer expirationDate = getExpirationDate(player);
			if (expirationDate == null) {
				return null;
			}
			return expirationDate - 1;
		}

		Integer startDate = getStartDateScore(player);
		if (startDate == null) {
			return null;
		}
		return (int) DateUtils.getWeeklyVersionEndDate(DateUtils.localDateTime(startDate));
	}

	public @Nullable Integer getInviteDaysRemaining(Player player) {
		return getInviteDaysRemaining(player, (int) DateUtils.getDaysSinceEpoch());
	}

	public @Nullable Integer getInviteDaysRemaining(Player player, int date) {
		Integer lastInviteDate = getLastInviteDate(player);
		if (lastInviteDate == null) {
			return null;
		}
		return lastInviteDate - date + 1;
	}

	public boolean canInvite(Player player) {
		return canInvite(player, (int) DateUtils.getDaysSinceEpoch());
	}

	public boolean canInvite(Player player, int date) {
		if (isChallenge(player)) {
			return false;
		}

		if (mCanAlwaysInvite) {
			return true;
		}

		Integer inviteDaysRemaining = getInviteDaysRemaining(player, date);
		return inviteDaysRemaining != null && inviteDaysRemaining > 0;
	}

	public boolean canAlwaysAbandon() {
		return mCanAlwaysAbandon;
	}

	public boolean canAbandon(Player player) {
		return canAbandon(player, (int) DateUtils.getDaysSinceEpoch());
	}

	public boolean canAbandon(Player player, int date) {
		if (mCanAlwaysAbandon) {
			return true;
		}

		Integer inviteDaysRemaining = getInviteDaysRemaining(player, date);
		return inviteDaysRemaining != null && inviteDaysRemaining <= 0;
	}

	public @Nullable Integer daysUntilCanAbandon(Player player) {
		return daysUntilCanAbandon(player, (int) DateUtils.getDaysSinceEpoch());
	}

	public @Nullable Integer daysUntilCanAbandon(Player player, int date) {
		if (mCanAlwaysAbandon) {
			return 0;
		}

		return getInviteDaysRemaining(player, date);
	}

	public String getAccessName() {
		return mAccessName;
	}

	public int getAccessScore(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, mAccessName).orElse(0);
	}

	public @Nullable String getFinishedName() {
		return mFinishedName;
	}

	public @Nullable Integer getFinishedScore(Player player) {
		if (mFinishedName == null) {
			return null;
		}
		return ScoreboardUtils.getScoreboardValue(player, mFinishedName).orElse(0);
	}

	public @Nullable String getStartDateName() {
		return mStartDateName;
	}

	public @Nullable Integer getStartDateScore(Player player) {
		if (mStartDateName == null) {
			return null;
		}
		return ScoreboardUtils.getScoreboardValue(player, mStartDateName).orElse(0);
	}

	public @Nullable Integer getDaysIntoInstance(Player player) {
		return getDaysIntoInstance(player, (int) DateUtils.getDaysSinceEpoch());
	}

	public @Nullable Integer getDaysIntoInstance(Player player, int date) {
		Integer startDate = getStartDateScore(player);
		if (startDate == null) {
			return null;
		}

		return date - startDate + 1;
	}

	public @Nullable Integer getExpirationDate(Player player) {
		Integer startDate = getStartDateScore(player);
		Integer durationDays = getDurationDays();

		if (startDate == null || durationDays == null) {
			return null;
		}
		return startDate + durationDays;
	}

	public @Nullable Integer getDaysRemaining(Player player) {
		return getDaysRemaining(player, (int) DateUtils.getDaysSinceEpoch());
	}

	public @Nullable Integer getDaysRemaining(Player player, int date) {
		Integer expirationDate = getExpirationDate(player);
		if (expirationDate == null) {
			return null;
		}
		return expirationDate - date;
	}

	public @Nullable String getLastVisitName() {
		return mLastVisitName;
	}

	public @Nullable Integer getLastVisitScore(Player player) {
		if (mLastVisitName == null) {
			return null;
		}
		OptionalInt optScore = ScoreboardUtils.getScoreboardValue(player, mLastVisitName);
		return optScore.isEmpty() ? null : optScore.getAsInt();
	}

	public @Nullable Integer getCompletedAutoExpirationDate(Player player) {
		Integer finishedScore = getFinishedScore(player);
		if (finishedScore == null || finishedScore == 0) {
			return null;
		}

		Integer lastVisitDate = getLastVisitScore(player);
		Integer expirationDate = getExpirationDate(player);
		if (lastVisitDate == null || expirationDate == null) {
			return null;
		}

		return Math.min(lastVisitDate + 8, expirationDate);
	}

	public @Nullable Integer getCompletedAutoExpirationDaysRemaining(Player player) {
		return getCompletedAutoExpirationDaysRemaining(player, (int) DateUtils.getDaysSinceEpoch());
	}

	public @Nullable Integer getCompletedAutoExpirationDaysRemaining(Player player, int date) {
		Integer autoExpirationDate = getCompletedAutoExpirationDate(player);
		if (autoExpirationDate == null) {
			return null;
		}

		return autoExpirationDate - date;
	}

	public @Nullable Integer getDurationDays() {
		return mDurationDays;
	}

	public @Nullable String getTypeName() {
		return mTypeName;
	}

	public @Nullable DelvePreset getDelvePreset() {
		return mSavagePreset;
	}

	public String getAbandonShardName(Player player) {
		if (SKT.equals(this)) {
			if (player.getScoreboardTags().contains("SKTQuest")) {
				player.removeScoreboardTag("SKTQuest");
				return "isles";
			} else {
				return "ring";
			}
		}

		if (mTypeName != null) {
			if (
				mExaltedTypeScore != null
					&& ScoreboardUtils.getScoreboardValue(player, mTypeName).orElse(0) == mExaltedTypeScore
			) {
				return "ring";
			}
		}

		return mAbandonShardName;
	}

	public int getDelveBountyId() {
		return mDelveBountyId;
	}

	public static @Nullable DungeonCommandMapping getByShard(String name) {
		return BY_SHARD.get(name);
	}

	public static @Nullable DungeonCommandMapping getByDelveBounty(Player player) {
		int delveDungeonScore = ScoreboardUtils.getScoreboardValue(player, "DelveDungeon").orElse(0);
		if (delveDungeonScore <= 0) {
			return null;
		}

		for (DungeonCommandMapping dungeonMapping : values()) {
			if (delveDungeonScore != dungeonMapping.getDelveBountyId()) {
				continue;
			}

			return dungeonMapping;
		}

		return null;
	}

	public void checkPlayerAccess(Player player) {
		checkPlayerAccess(player, (int) DateUtils.getDaysSinceEpoch());
	}

	public void checkPlayerAccess(Player player, int date) {
		Integer durationDays = getDurationDays();
		if (durationDays == null || durationDays <= 0) {
			return;
		}

		String dungeonName = getLocationName();

		int accessScore = getAccessScore(player);
		if (accessScore == 0) {
			// Player has no instance
			return;
		}

		Integer startDate = getStartDateScore(player);
		if (startDate == null || startDate == 0) {
			// Invalid scores; force abandon
			forceAbandon(List.of(player));
			return;
		}

		Integer daysRemaining = getDaysRemaining(player, date);
		if (daysRemaining != null && daysRemaining <= 0) {
			// Instance expired
			player.sendMessage(Component.text("Your " + dungeonName + " instance has expired", NamedTextColor.RED));
			forceAbandon(List.of(player));
			return;
		}

		if (player.getScoreboardTags().contains("NoAutoDungeonAbandon")) {
			// Respect the PEB toggle for disabling auto-abandoning dungeon instances, at least for now
			return;
		}

		Integer finishedScore = getFinishedScore(player);
		if (finishedScore == null || finishedScore == 0) {
			// Not finished or possible to store as finished; ignore
			return;
		}

		Integer autoExpirationDaysRemaining = getCompletedAutoExpirationDaysRemaining(player, date);
		if (autoExpirationDaysRemaining != null && autoExpirationDaysRemaining <= 0) {
			// Auto-kick players who completed their instance, but haven't visited in a while
			player.sendMessage(Component.text("Your completed " + dungeonName + " instance has expired", NamedTextColor.RED));
			forceAbandon(List.of(player));
		}
	}

	public List<Component> getDungeonAccessTimeInfo(Player player) {
		return getDungeonAccessTimeInfo(player, (int) DateUtils.getDaysSinceEpoch());
	}

	public List<Component> getDungeonAccessTimeInfo(Player player, int date) {
		String dungeonName = getShardName();
		Integer durationDays = getDurationDays();

		if (durationDays == null) {
			// Not an auto-expiring instance
			return List.of();
		}

		Integer startDate = getStartDateScore(player);
		if (startDate == null || startDate == 0) {
			// Invalid scores
			return List.of();
		}
		Integer inviteDaysRemaining = getInviteDaysRemaining(player, date);
		boolean canInvite = canInvite(player, date);
		boolean canAbandon = canAbandon(player, date);
		Integer daysIntoInstance = getDaysIntoInstance(player, date);
		Integer daysRemaining = getDaysRemaining(player, date);

		if (inviteDaysRemaining == null || daysIntoInstance == null || daysRemaining == null || daysRemaining <= 0) {
			// Instance expired; message appears soon after checking this, so no need to put it here
			return List.of();
		}

		TextColor invitableColor = canInvite ? NamedTextColor.GREEN : NamedTextColor.AQUA;
		List<Component> lines = new ArrayList<>();

		lines.add(Component.text("This instance is on day ", invitableColor)
			.decoration(TextDecoration.ITALIC, false)
			.append(Component.text(daysIntoInstance, NamedTextColor.GOLD, TextDecoration.BOLD))
			.append(Component.text(".")));

		if (canInvite) {
			if (canAlwaysInvite()) {
				lines.add(Component.text("You can invite your friends with no " + dungeonName + " instance at any time.", NamedTextColor.GREEN)
					.decoration(TextDecoration.ITALIC, false));
			} else if (inviteDaysRemaining == 1) {
				lines.add(Component.text("You can invite your friends with no " + dungeonName + " instance for the rest of today.", NamedTextColor.GREEN)
					.decoration(TextDecoration.ITALIC, false));
			} else {
				lines.add(Component.text("You can invite your friends with no " + dungeonName + " instance for the next ", NamedTextColor.GREEN)
					.decoration(TextDecoration.ITALIC, false)
					.append(Component.text(inviteDaysRemaining, NamedTextColor.GOLD, TextDecoration.BOLD))
					.append(Component.text(" days.")));
			}
		}

		if (canAbandon) {
			lines.add(Component.text("You can abandon your " + dungeonName + " instance.", NamedTextColor.AQUA)
				.decoration(TextDecoration.ITALIC, false));
		}

		if (daysRemaining == 1) {
			lines.add(Component.text("Today is the last day you have access to this " + dungeonName + " instance.", NamedTextColor.DARK_RED)
				.decoration(TextDecoration.ITALIC, false));
		} else if (daysRemaining == 2) {
			lines.add(Component.text("Tomorrow is the last day you have access to this " + dungeonName + " instance.", NamedTextColor.DARK_RED)
				.decoration(TextDecoration.ITALIC, false));
		} else if (daysRemaining <= 7) {
			lines.add(Component.text("You have ", NamedTextColor.RED)
				.decoration(TextDecoration.ITALIC, false)
				.append(Component.text(daysRemaining, NamedTextColor.GOLD, TextDecoration.BOLD))
				.append(Component.text(" days of access to " + dungeonName + " remaining.")));
		} else {
			TextColor greenOrYellow = canInvite ? NamedTextColor.GREEN : NamedTextColor.YELLOW;
			lines.add(Component.text("You have ", greenOrYellow)
				.decoration(TextDecoration.ITALIC, false)
				.append(Component.text(daysRemaining, NamedTextColor.GOLD, TextDecoration.BOLD))
				.append(Component.text(" days of access to " + dungeonName + " remaining.")));
		}

		Integer daysUntilAutoAbandon = getCompletedAutoExpirationDaysRemaining(player, date);

		if (daysUntilAutoAbandon != null && !daysUntilAutoAbandon.equals(daysRemaining)) {
			if (player.getScoreboardTags().contains("NoAutoDungeonAbandon")) {
				lines.add(Component.text("You have completed " + dungeonName + ", and disabled auto-abandoning completed instances.", NamedTextColor.AQUA)
					.decoration(TextDecoration.ITALIC, false));
			} else if (daysUntilAutoAbandon == 1) {
				lines.add(Component.text("If you do not visit your completed " + dungeonName + " instance today, you will lose access to it.", NamedTextColor.DARK_RED)
					.decoration(TextDecoration.ITALIC, false));
			} else if (daysUntilAutoAbandon == 2) {
				lines.add(Component.text("If you do not visit your completed " + dungeonName + " instance by tomorrow, you will lose access to it.", NamedTextColor.RED)
					.decoration(TextDecoration.ITALIC, false));
			} else if (daysUntilAutoAbandon > 2) {
				lines.add(Component.text("If you do not visit your completed " + dungeonName + " in the next ", NamedTextColor.YELLOW)
					.decoration(TextDecoration.ITALIC, false)
					.append(Component.text(daysUntilAutoAbandon, NamedTextColor.GOLD, TextDecoration.BOLD))
					.append(Component.text(" days, you will lose access to it.")));
			}
		}

		return lines;
	}

	/**
	 * Abandons a dungeon instance. Does not perform a date check to see if the instance can be abandoned.
	 */
	public void forceAbandon(Collection<Player> players) {
		String shardName = getShardName();
		if (shardName != null && DelvesManager.DUNGEONS.contains(shardName)) {
			for (Player player : players) {
				DelvesUtils.clearDelvePlayerByShard(null, player, shardName);

				// Abandon delve bounty if it is for the abandoned dungeon and the dungeon is of the same week as the delve bounty
				if (getDelveBountyId() > 0 && getStartDateName() != null
					&& ScoreboardUtils.getScoreboardValue(player, getAccessName()).orElse(0) != 0
					&& ScoreboardUtils.getScoreboardValue(player, "DelveDungeon").orElse(0) == getDelveBountyId()) {
					int delveBountyStartDate = ScoreboardUtils.getScoreboardValue(player, "DelveStartDate").orElse(0);
					int startDate = ScoreboardUtils.getScoreboardValue(player, getStartDateName()).orElse(0);

					long thisWeek = DateUtils.getWeeklyVersion();
					long startWeek = DateUtils.getWeeklyVersion(startDate);
					long bountyWeek = DateUtils.getWeeklyVersion(delveBountyStartDate);
					if (thisWeek > startWeek && bountyWeek == startWeek) {
						ScoreboardUtils.setScoreboardValue(player, "DelveDungeon", 0);
						ScoreboardUtils.setScoreboardValue(player, "DelveStartDate", 0);
					}
				}
			}
		}

		for (Player player : players) {
			// Get the shard to return players to before resetting their access scores
			String abandonShardName = getAbandonShardName(player);

			if (getFinishedName() != null) {
				ScoreboardUtils.setScoreboardValue(player, getFinishedName(), 0);
			}
			if (getStartDateName() != null) {
				ScoreboardUtils.setScoreboardValue(player, getStartDateName(), 0);
			}
			if (getTypeName() != null) {
				ScoreboardUtils.setScoreboardValue(player, getTypeName(), 0);
			}
			ScoreboardUtils.setScoreboardValue(player, getAccessName(), 0);

			// boot them out if they are on this shard
			if (shardName != null && ServerProperties.getShardName().contains(shardName)) {
				// Send players to the overworld of the shard, usually the sort box
				try {
					MonumentaWorldManagementAPI.sortWorld(player);
				} catch (Exception ex) {
					MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), ex);
				}

				try {
					ShardSorterCommand.sortToShard(player, abandonShardName, null);
				} catch (Exception ex) {
					MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), ex);
				}
			}
		}
	}

	public void delveBountyAbandonCheck(Player player) {
		long currentWeek = DateUtils.getWeeklyVersion();

		int delveBountyStartDate = ScoreboardUtils.getScoreboardValue(player, "DelveStartDate").orElse(0);
		long delveBountyWeek = DateUtils.getWeeklyVersion(delveBountyStartDate);
		if (currentWeek == delveBountyWeek) {
			// Still a chance to start the delve dungeon!
			return;
		}

		String startDateObjective = getStartDateName();
		String shardName = getShardName();
		if (startDateObjective == null || shardName == null) {
			MMLog.warning("Null start date or shard name for delve bounty dungeon " + this + "!");
			return;
		}

		int startDate = ScoreboardUtils.getScoreboardValue(player, startDateObjective).orElse(0);
		long dungeonWeek = DateUtils.getWeeklyVersion(startDate);

		if (startDate == 0 || delveBountyWeek != dungeonWeek) {
			// No instance for that week
			abandonDelve(player);
			return;
		}

		DelvesManager.DungeonDelveInfo delveInfo = DelvesUtils.getDelveInfo(player, shardName);
		int totalPoints = DelvesUtils.getTotalPoints(delveInfo.getMap());
		if (totalPoints == 0) {
			// Not a delve dungeon
			abandonDelve(player);
		}
	}

	public static void abandonDelve(Player player) {
		ScoreboardUtils.setScoreboardValue(player, "DelveDungeon", 0);
		ScoreboardUtils.setScoreboardValue(player, "DelveStartDate", 0);
		player.sendMessage(Component.text(
			"Your delve bounty expired - talk to Knight-Errant Reynart for another!", NamedTextColor.AQUA));
	}

	public void showHeadsUpMessage(Collection<Player> players) {
		if (players.isEmpty()) {
			// No one to summarize
			return;
		}

		Component header = Component.empty()
			.append(Component.text("[-_-] ", NamedTextColor.GOLD));
		int date = (int) DateUtils.getDaysSinceEpoch();

		Map<Player, PlayerDungeonStatus> playerStatuses = new HashMap<>();
		for (Player player : players) {
			playerStatuses.put(player, new PlayerDungeonStatus(player, this, date));
		}

		for (Map.Entry<Player, PlayerDungeonStatus> ownEntry : playerStatuses.entrySet()) {
			Player player = ownEntry.getKey();
			PlayerDungeonStatus ownStatus = ownEntry.getValue();

			boolean shouldPlayErrorSound = false;
			boolean shouldPlayWarningSound = false;

			List<String> ownInstance = new ArrayList<>();
			List<String> noInstance = new ArrayList<>();
			List<String> otherNoAbandonNoInvite = new ArrayList<>();
			List<String> otherNoAbandonYesInvite = new ArrayList<>();
			List<String> otherYesAbandonNoInvite = new ArrayList<>();
			List<String> otherYesAbandonYesInvite = new ArrayList<>();

			for (Player otherPlayer : PlayerUtils.playerSortedByNames(players)) {
				if (otherPlayer.equals(player)) {
					continue;
				}

				PlayerDungeonStatus otherStatus = playerStatuses.get(otherPlayer);
				if (otherStatus == null) {
					// Impossible, but makes null checks happy
					continue;
				}

				if (!otherStatus.mHasInstance) {
					noInstance.add(otherStatus.mPlayerName);

					if (ownStatus.mHasInstance && !ownStatus.mCanInvite) {
						if (ownStatus.mCanAbandon) {
							shouldPlayWarningSound = true;
						} else {
							shouldPlayErrorSound = true;
						}
					}
				} else if (ownStatus.mAccessScore == otherStatus.mAccessScore) {
					// Same instance, or neither has an instance

					if (ownStatus.mHasInstance) {
						ownInstance.add(otherStatus.mPlayerName);
					}
				} else {
					// Other player is not in the same instance, target player may or may not have one

					if (otherStatus.mCanAbandon) {
						if (otherStatus.mCanInvite) {
							otherYesAbandonYesInvite.add(otherStatus.mPlayerName);
						} else {
							otherYesAbandonNoInvite.add(otherStatus.mPlayerName);
						}

						if (ownStatus.mHasInstance) {
							if (ownStatus.mCanInvite || ownStatus.mCanAbandon) {
								shouldPlayWarningSound = true;
							} else {
								shouldPlayErrorSound = true;
							}
						}
					} else {
						// Other player can't abandon

						if (otherStatus.mCanInvite) {
							otherNoAbandonYesInvite.add(otherStatus.mPlayerName);

							if (ownStatus.mHasInstance) {
								if (ownStatus.mCanAbandon) {
									shouldPlayWarningSound = true;
								} else {
									shouldPlayErrorSound = true;
								}
							}
						} else {
							otherNoAbandonNoInvite.add(otherStatus.mPlayerName);

							if (ownStatus.mHasInstance) {
								shouldPlayErrorSound = true;
							}
						}
					}
				}
			}

			player.sendMessage(header.append(Component.text("Just a heads up:")));
			if (ownStatus.mHasInstance) {
				Component ownInviteStatus;
				if (ownStatus.mCanInvite) {
					ownInviteStatus = Component.text("can invite other players", NamedTextColor.GREEN);
				} else {
					ownInviteStatus = Component.text("cannot invite other players", NamedTextColor.RED);
				}
				Component ownAbandonStatus;
				if (ownStatus.mCanAbandon) {
					ownAbandonStatus = Component.text("can abandon it", NamedTextColor.GREEN);
				} else {
					ownAbandonStatus = Component.text("cannot abandon it", NamedTextColor.RED);
				}

				player.sendMessage(header
					.append(Component.text("You have an instance, you "))
					.append(ownInviteStatus)
					.append(Component.text(", and "))
					.append(ownAbandonStatus)
				);
			} else {
				player.sendMessage(header.append(Component.text("You have no instance")));
			}

			if (players.size() == 1) {
				// No other players to summarize
				return;
			}

			player.sendMessage(header.append(Component.text("As for everyone else here:")));
			if (!ownInstance.isEmpty()) {
				player.sendMessage(header
					.append(Component.text("People in your instance: "
						+ MessagingUtils.concatenateStringsWithAnd(ownInstance), NamedTextColor.GREEN))
				);
			}
			if (!noInstance.isEmpty()) {
				player.sendMessage(header
					.append(Component.text("People with no instance: "
						+ MessagingUtils.concatenateStringsWithAnd(noInstance), NamedTextColor.GREEN))
				);
			}
			if (!otherYesAbandonYesInvite.isEmpty()) {
				player.sendMessage(header
					.append(Component.text("People who can "))
					.append(Component.text("abandon", NamedTextColor.GREEN))
					.append(Component.text(" or "))
					.append(Component.text("invite people to", NamedTextColor.GREEN))
					.append(Component.text(" their instance: "))
					.append(Component.text(MessagingUtils.concatenateStringsWithAnd(otherYesAbandonYesInvite), NamedTextColor.GREEN))
				);
			}
			if (!otherYesAbandonNoInvite.isEmpty()) {
				player.sendMessage(header
					.append(Component.text("People who can "))
					.append(Component.text("abandon", NamedTextColor.GREEN))
					.append(Component.text(" but "))
					.append(Component.text("not invite people to", NamedTextColor.RED))
					.append(Component.text(" their instance: "))
					.append(Component.text(MessagingUtils.concatenateStringsWithAnd(otherYesAbandonNoInvite), NamedTextColor.GREEN))
				);
			}
			if (!otherNoAbandonYesInvite.isEmpty()) {
				player.sendMessage(header
					.append(Component.text("People who can "))
					.append(Component.text("not abandon", NamedTextColor.RED))
					.append(Component.text(" but "))
					.append(Component.text("can invite people to", NamedTextColor.GREEN))
					.append(Component.text(" their instance: "))
					.append(Component.text(MessagingUtils.concatenateStringsWithAnd(otherNoAbandonYesInvite),
						ownStatus.mHasInstance ? NamedTextColor.RED : NamedTextColor.GREEN))
				);
			}
			if (!otherNoAbandonNoInvite.isEmpty()) {
				player.sendMessage(header
					.append(Component.text("People who can "))
					.append(Component.text("not abandon", NamedTextColor.RED))
					.append(Component.text(" nor "))
					.append(Component.text("invite people to", NamedTextColor.RED))
					.append(Component.text(" their instance: "))
					.append(Component.text(MessagingUtils.concatenateStringsWithAnd(otherNoAbandonNoInvite),
						ownStatus.mHasInstance ? NamedTextColor.RED : NamedTextColor.GREEN))
				);
			}

			if (shouldPlayErrorSound) {
				player.playSound(player, Sound.ENTITY_SHULKER_DEATH, SoundCategory.HOSTILE, 1.0f, 1.0f);
			} else if (shouldPlayWarningSound) {
				player.playSound(player, Sound.ENTITY_SHULKER_HURT, SoundCategory.HOSTILE, 1.0f, 1.0f);
			}
		}
	}

	public static class PlayerDungeonStatus {
		public final UUID mPlayerId;
		public final String mPlayerName;
		public final DungeonCommandMapping mDungeonMapping;
		public final int mAccessScore;
		public final @Nullable Integer mStartDate;
		public final @Nullable Integer mInviteDaysRemaining;
		public final @Nullable Integer mFinishScore;
		public final @Nullable Integer mMinDaysToManualAbandon;
		public final @Nullable Integer mMinDaysToAutoExpiration;
		public final @Nullable Integer mMaxDaysToAutoExpiration;
		public final int mDelvePoints;
		public final boolean mHasInstance;
		public final boolean mCanInvite;
		public final boolean mCanAbandon;
		public final boolean mIsDelveBounty;
		public final boolean mIsChallenge;
		public final boolean mIsExalted;
		public final @Nullable DelvePreset mDelvePreset;

		public PlayerDungeonStatus(Player player, DungeonCommandMapping dungeonMapping) {
			this(player, dungeonMapping, (int) DateUtils.getDaysSinceEpoch());
		}

		public PlayerDungeonStatus(Player player, DungeonCommandMapping dungeonMapping, int date) {
			mPlayerId = player.getUniqueId();
			mPlayerName = player.getName();
			mDungeonMapping = dungeonMapping;
			mAccessScore = dungeonMapping.getAccessScore(player);
			mHasInstance = mAccessScore > 0;
			mStartDate = dungeonMapping.getStartDateScore(player);
			mCanInvite = dungeonMapping.canInvite(player, date);
			mInviteDaysRemaining = dungeonMapping.getInviteDaysRemaining(player, date);
			mFinishScore = dungeonMapping.getFinishedScore(player);
			mCanAbandon = dungeonMapping.canAbandon(player, date);
			mMinDaysToManualAbandon = dungeonMapping.daysUntilCanAbandon(player, date);
			mMinDaysToAutoExpiration = dungeonMapping.getCompletedAutoExpirationDaysRemaining(player, date);
			mMaxDaysToAutoExpiration = dungeonMapping.getDaysRemaining(player, date);
			mIsDelveBounty = dungeonMapping.equals(getByDelveBounty(player));
			mIsChallenge = dungeonMapping.isChallenge(player);
			mIsExalted = dungeonMapping.isExalted(player);

			String shardName = dungeonMapping.getShardName();
			if (shardName == null) {
				mDelvePoints = 0;
				mDelvePreset = null;
			} else {
				DelvesManager.DungeonDelveInfo delveInfo = DelvesUtils.getDelveInfo(player, shardName);
				mDelvePoints = DelvesUtils.getTotalPoints(delveInfo.getMap());

				mDelvePreset = DelvePreset.getDelvePreset(delveInfo.mPresetId);
			}
		}
	}
}
