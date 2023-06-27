package com.playmonumenta.plugins.seasonalevents;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.seasonalevents.gui.PassGui;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SeasonalEventManager {
	private static class PassFiles {
		public final LocalDateTime mPassStart;
		public @Nullable JsonObject mMissionsJson = null;
		public @Nullable JsonObject mRewardsJson = null;

		public PassFiles(LocalDateTime passStart) {
			mPassStart = passStart;
		}
	}

	// The first week of the rework; used to mark old progress as unknown
	public static final long PLAYER_PROGRESS_REWORK_WEEK = DateUtils.getWeeklyVersion(
		LocalDateTime.of(2023, 6, 30, 0, 0));
	public static final String ALL_MISSIONS_TITLE_NAME = "Tryhard";
	public static final String SEASONAL_PASSES_FOLDER = "seasonalevents";
	public static TreeMap<LocalDateTime, SeasonalPass> mAllPasses = new TreeMap<>();
	public static @Nullable SeasonalPass mActivePass = null;
	private static final Map<UUID, PlayerProgress> mPlayerProgress = new HashMap<>();

	public SeasonalEventManager() {
		reloadPasses(Bukkit.getConsoleSender());
	}

	public static void reloadPasses(final CommandSender sender) {
		Plugin plugin = Plugin.getInstance();
		final TreeMap<String, PassFiles> allPassFiles = new TreeMap<>();
		LocalDateTime now = DateUtils.localDateTime();
		LocalDateTime mostRecentPassStart = LocalDateTime.MIN;

		sender.sendMessage(Component.text("Loading all seasonalevent files (files without start_date incorrectly counted):", NamedTextColor.GOLD));
		QuestUtils.loadScriptedQuests(plugin,
			SEASONAL_PASSES_FOLDER,
			sender,
			(JsonObject object, File file) -> {
				@Nullable JsonElement startDateJson = object.get("start_date");
				if (startDateJson == null) {
					// Invalid seasonal pass file, ignore
					String msg = "No start_date in " + file;
					sender.sendMessage(Component.text(msg, NamedTextColor.RED));
					throw new Exception(msg);
				}
				String startDateString = startDateJson.getAsString();
				PassFiles passFiles = allPassFiles.computeIfAbsent(startDateString, (key) -> {
					LocalDateTime passStart = LocalDateTime.parse(startDateString + "T00:00:00");
					return new PassFiles(passStart);
				});

				if (object.has("missions")) {
					passFiles.mMissionsJson = object;
					return file.getName() + "_missions";
				}
				if (object.has("rewards")) {
					passFiles.mRewardsJson = object;
					return file.getName() + "_rewards";
				}

				String msg = "File is neither missions nor rewards file: " + file;
				sender.sendMessage(Component.text(msg, NamedTextColor.RED));
				throw new Exception(msg);
			});

		for (Map.Entry<String, PassFiles> passFilesEntry : allPassFiles.entrySet()) {
			LocalDateTime passStart = passFilesEntry.getValue().mPassStart;
			if (passStart.isBefore(now) && passStart.isAfter(mostRecentPassStart)) {
				mostRecentPassStart = passStart;
			}
		}

		sender.sendMessage(Component.text("Parsing passes:", NamedTextColor.GOLD));
		final TreeMap<LocalDateTime, SeasonalPass> allPasses = new TreeMap<>();
		for (Map.Entry<String, PassFiles> passFilesEntry : allPassFiles.entrySet()) {
			String startDateString = passFilesEntry.getKey();
			PassFiles passFiles = passFilesEntry.getValue();
			if (passFiles.mMissionsJson == null) {
				sender.sendMessage(Component.text("Pass missions file missing for " + startDateString, NamedTextColor.RED));
				continue;
			}
			if (passFiles.mRewardsJson == null) {
				sender.sendMessage(Component.text("Pass rewards file missing for " + startDateString, NamedTextColor.RED));
				continue;
			}
			try {
				SeasonalPass seasonalPass = new SeasonalPass(sender, passFiles.mPassStart, passFiles.mMissionsJson, passFiles.mRewardsJson, !passFiles.mPassStart.isBefore(mostRecentPassStart));
				allPasses.put(seasonalPass.mPassStart, seasonalPass);
				sender.sendMessage(Component.text("Loaded " + seasonalPass, NamedTextColor.GOLD));
			} catch (Exception exception) {
				MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), exception);
			}
		}

		mAllPasses = allPasses;
		mActivePass = getPass(DateUtils.localDateTime());
		if (mActivePass != null) {
			sender.sendMessage(Component.text("Active pass: " + mActivePass, NamedTextColor.GOLD));
		} else {
			sender.sendMessage(Component.text("There is no currently active season pass", NamedTextColor.GOLD));
		}

		PassGui.refreshOpenGuis();
	}

	public static @Nullable SeasonalPass getPass() {
		return mActivePass;
	}

	public static @Nullable SeasonalPass getPass(LocalDateTime localDateTime) {
		@Nullable Map.Entry<LocalDateTime, SeasonalPass> seasonalPassEntry = mAllPasses.floorEntry(localDateTime);
		if (seasonalPassEntry == null) {
			return null;
		}
		SeasonalPass seasonalPass = seasonalPassEntry.getValue();
		if (seasonalPass.isActive(localDateTime)) {
			return seasonalPass;
		}
		return null;
	}

	public static @Nullable SeasonalPass getMostRecentPass() {
		return getMostRecentPass(DateUtils.localDateTime());
	}

	public static @Nullable SeasonalPass getMostRecentPass(LocalDateTime localDateTime) {
		@Nullable Map.Entry<LocalDateTime, SeasonalPass> seasonalPassEntry = mAllPasses.floorEntry(localDateTime);
		if (seasonalPassEntry == null) {
			return null;
		}
		return seasonalPassEntry.getValue();
	}

	/**
	 * Runs through the missions list and returns all
	 * matching the current week number of the pass
	 */
	public static List<WeeklyMission> getActiveMissions() {
		if (mActivePass != null && mActivePass.isActive()) {
			return mActivePass.getActiveMissions();
		}
		return new ArrayList<>();
	}

	public static int getMP(Player p) {
		if (mActivePass != null && mActivePass.isActive()) {
			return mActivePass.getMP(p);
		}
		return 0;
	}

	/**
	 * Triggered by multiple event handlers, this method adds an amount of progress
	 * to the provided weekly mission number for that week
	 */
	public static void addWeeklyMissionProgress(Player p, int missionNumber, int amount) {
		if (mActivePass != null && mActivePass.isActive()) {
			mActivePass.addWeeklyMissionProgress(p, missionNumber, amount);
		}
	}

	// Time Utils

	public static @Nullable PlayerProgress getPlayerProgress(Player player) {
		return mPlayerProgress.get(player.getUniqueId());
	}

	public static void overwritePlayerProgress(Player player, PlayerProgress playerProgress) {
		mPlayerProgress.put(player.getUniqueId(), playerProgress);
	}

	public static void unloadPlayerData(@NotNull Player player) {
		mPlayerProgress.remove(player.getUniqueId());
	}

	public static void loadPlayerProgressJson(Player player, @Nullable JsonElement data) {
		UUID playerId = player.getUniqueId();
		if (data instanceof JsonObject playerProgressJson) {
			mPlayerProgress.put(playerId, new PlayerProgress(player, playerProgressJson));
		} else {
			mPlayerProgress.put(playerId, new PlayerProgress());
		}
	}

	public static JsonObject getPlayerProgressJson(Player player) {
		UUID playerId = player.getUniqueId();
		PlayerProgress playerProgress = mPlayerProgress.get(playerId);
		if (playerProgress == null) {
			return new JsonObject();
		}
		return playerProgress.toJson();
	}
}
