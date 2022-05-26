package com.playmonumenta.plugins.seasonalevents;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class SeasonalEventManager {
	private static class PassFiles {
		public @Nullable JsonObject mMissionsJson = null;
		public @Nullable JsonObject mRewardsJson = null;
	}

	public static final int LEVEL_COUNT = 25;
	public static final int MP_PER_LEVEL = 75;
	public static final String MISSION_SCOREBOARD = "WeeklyMission";
	public static final String ALL_MISSIONS_TITLE_NAME = "Tryhard";
	public static final String SEASONAL_PASSES_FOLDER = "seasonalevents";
	public static TreeMap<LocalDateTime, SeasonalPass> mAllPasses = new TreeMap<>();
	public static @Nullable SeasonalPass mActivePass = null;

	public SeasonalEventManager() {
		reloadPasses(Bukkit.getConsoleSender());
	}

	public static void reloadPasses(final CommandSender sender) {
		Plugin plugin = Plugin.getInstance();
		final TreeMap<String, PassFiles> allPassFiles = new TreeMap<>();

		sender.sendMessage(Component.text("Loading all seasonalevent files (files without start_date incorrectly counted):", NamedTextColor.GOLD));
		QuestUtils.loadScriptedQuests(plugin,
			SEASONAL_PASSES_FOLDER,
			sender,
			(object, file) -> {
				@Nullable JsonElement startDateJson = object.get("start_date");
				if (startDateJson == null) {
					// Invalid seasonal pass file, ignore
					sender.sendMessage(Component.text("No start_date in " + file, NamedTextColor.RED));
					return null;
				}
				String startDateString = startDateJson.getAsString();
				PassFiles passFiles = allPassFiles.computeIfAbsent(startDateString, (key) -> new PassFiles());

				if (object.has("missions")) {
					passFiles.mMissionsJson = object;
				}
				if (object.has("rewards")) {
					passFiles.mRewardsJson = object;
				}
				return null;
			});
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
				SeasonalPass seasonalPass = new SeasonalPass(startDateString, passFiles.mMissionsJson, passFiles.mRewardsJson);
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
	}

	public static void handlePlayerDailyChange(Player p) {
		if (mActivePass != null && mActivePass.isActive()) {
			int currentPassWeek = mActivePass.getWeekOfPass();
			int playerLastDailyVersion = ScoreboardUtils.getScoreboardValue(p, "DailyVersion").orElse(0);
			LocalDateTime lastPlayedDate = DateUtils.localDateTime(playerLastDailyVersion);
			int lastPlayedWeek = mActivePass.getWeekOfPass(lastPlayedDate);
			if (lastPlayedWeek != currentPassWeek) {
				mActivePass.resetPlayerPassMissions(p);
			}
			if (lastPlayedWeek < 1) {
				// Player has not played during this pass yet, reset their MP
				ScoreboardUtils.setScoreboardValue(p, mActivePass.PASS_MP_SCOREBOARD, 0);
			}
		}
	}

	public static SeasonalPass getPass() {
		return mActivePass;
	}

	public static @Nullable SeasonalPass getPass(LocalDateTime localDateTime) {
		@Nullable Map.Entry<LocalDateTime, SeasonalPass> seasonalPassEntry = SeasonalEventManager.mAllPasses.floorEntry(localDateTime);
		if (seasonalPassEntry == null) {
			return null;
		}
		SeasonalPass seasonalPass = seasonalPassEntry.getValue();
		if (seasonalPass.isActive(localDateTime)) {
			return seasonalPass;
		}
		return null;
	}

	/**
	 * Adds a certain amount of MP to the player
	 * If above the threshold for a new unlock, awards it to the player
	 */
	public static void addMP(Player p, int amount) {
		if (mActivePass != null && mActivePass.isActive()) {
			mActivePass.addMP(p, amount);
		}
	}

	/**
	 * This method is called before opening the GUI to view the battle pass and updates player MP
	 * with any new earned MP from completed missions
	 */
	public static void updatePlayerPassProgress(Player p) {
		if (mActivePass != null && mActivePass.isActive()) {
			mActivePass.updatePlayerPassProgress(p);
		}
	}

	public static int getWeekOfPass() {
		if (mActivePass != null && mActivePass.isActive()) {
			return mActivePass.getWeekOfPass();
		}
		return 1;
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

	/**
	 * Debug method to get missions for given week
	 */
	public static List<WeeklyMission> getMissionsInWeek(int week) {
		if (mActivePass != null && mActivePass.isActive()) {
			return mActivePass.getMissionsInWeek(week);
		}
		return new ArrayList<>();
	}

	public static int getMP(Player p) {
		if (mActivePass != null && mActivePass.isActive()) {
			return mActivePass.getMP(p);
		}
		return 0;
	}

	public static void setMP(Player p, int amount) {
		if (mActivePass != null && mActivePass.isActive()) {
			mActivePass.setMP(p, amount);
		}
	}

	public static int getLevelFromMP(int mp) {
		if (mActivePass != null && mActivePass.isActive()) {
			return mActivePass.getLevelFromMP(mp);
		}
		return 0;
	}

	/**
	 * Triggered by multiple event handlers, this method adds an amount of progress
	 * to the provided weekly mission number for that week
	 */
	public static void addWeeklyMissionProgress(Player p, WeeklyMission mission, int missionNumber, int amount) {
		if (mActivePass != null && mActivePass.isActive()) {
			mActivePass.addWeeklyMissionProgress(p, mission, missionNumber, amount);
		}
	}

	public static void animate(Player player) {
		Location loc = player.getLocation();
		Firework fw = (Firework) player.getWorld().spawnEntity(loc, EntityType.FIREWORK);
		FireworkMeta fwm = fw.getFireworkMeta();
		FireworkEffect.Builder fwBuilder = FireworkEffect.builder();
		fwBuilder.withColor(Color.RED, Color.GREEN, Color.BLUE);
		fwBuilder.with(FireworkEffect.Type.BURST);
		FireworkEffect fwEffect = fwBuilder.build();
		fwm.addEffect(fwEffect);
		fw.setFireworkMeta(fwm);

		new BukkitRunnable() {
			@Override
			public void run() {
				fw.detonate();
			}
		}.runTaskLater(Plugin.getInstance(), 5);
	}

	// Time Utils

	public static int getDaysUntilMissionReset() {
		if (mActivePass != null && mActivePass.isActive()) {
			return mActivePass.getDaysUntilMissionReset();
		}
		return 0;
	}

	public static int getHoursUntilMissionReset() {
		if (mActivePass != null && mActivePass.isActive()) {
			return mActivePass.getHoursUntilMissionReset();
		}
		return 0;
	}

	public static int getDaysUntilPassEnd() {
		if (mActivePass != null && mActivePass.isActive()) {
			return mActivePass.getDaysUntilPassEnd();
		}
		return 0;
	}

	public static int getHoursUntilPassEnd() {
		if (mActivePass != null && mActivePass.isActive()) {
			return mActivePass.getHoursUntilPassEnd();
		}
		return 0;
	}
}
