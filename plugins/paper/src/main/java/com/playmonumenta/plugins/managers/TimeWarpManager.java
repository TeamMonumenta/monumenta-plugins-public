package com.playmonumenta.plugins.managers;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.seasonalevents.SeasonalEventManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.server.reset.DailyReset;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class TimeWarpManager {
	public static String CONFIG_NAME = "time_warp.json";
	private static boolean mPluginIsLoaded = false;
	private static long mSecondOffset = 0;

	public static void reset() {
		set(0);
	}

	public static void add(long amount, TemporalUnit unit) {
		set(DateUtils.localDateTime().plus(amount, unit));
	}

	public static void set(LocalDateTime desiredUtcTime) {
		set(desiredUtcTime.toEpochSecond((ZoneOffset) DateUtils.TIMEZONE)
			- DateUtils.trueLocalDateTime().toEpochSecond((ZoneOffset) DateUtils.TIMEZONE));
	}

	public static void set(long offsetSeconds) {
		mSecondOffset = offsetSeconds;
		save();
	}

	public static long get() {
		return mSecondOffset;
	}

	public static void load() {
		if (!Plugin.ENABLE_TIME_WARP) {
			reset();
			return;
		}

		File configFile = getConfigFile();
		try {
			JsonObject config = FileUtils.readJson(configFile.getAbsolutePath());
			set(config.get("offset_seconds").getAsLong());
		} catch (Exception e) {
			MMLog.warning("Failed to load " + CONFIG_NAME + ": " + e.getMessage());
			reset();
		}
		mPluginIsLoaded = true;
	}

	public static void unload() {
		mPluginIsLoaded = false;
	}

	private static void save() {
		Plugin plugin = Plugin.getInstance();
		CommandSender sender = Bukkit.getConsoleSender();
		if (mPluginIsLoaded) {
			// Update real-time-based plugin code
			DailyReset.timeWarp(plugin);
			SeasonalEventManager.reloadPasses(sender);
			Bukkit.getServer().sendMessage(Component.text("Let's do the time warp again!", NamedTextColor.GOLD, TextDecoration.BOLD));
		}

		JsonObject config = new JsonObject();
		config.addProperty("offset_seconds", mSecondOffset);
		try {
			FileUtils.writeJson(getConfigFile().getAbsolutePath(), config);
		} catch (IOException e) {
			MMLog.warning("Failed to save " + CONFIG_NAME + ": " + e.getMessage());
		}
	}

	private static File getConfigFile() {
		return new File(Plugin.getInstance().getDataFolder(), CONFIG_NAME);
	}
}
