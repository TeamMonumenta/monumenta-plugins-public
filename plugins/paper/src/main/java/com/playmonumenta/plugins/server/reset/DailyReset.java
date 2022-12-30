package com.playmonumenta.plugins.server.reset;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.poi.POIManager;
import com.playmonumenta.plugins.seasonalevents.SeasonalEventManager;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.Nullable;

public class DailyReset {
	private static final TreeSet<Integer> COUNTDOWN_SECONDS = new TreeSet<>(Arrays.asList(
		60 * 60 * 2,
		60 * 60,
		60 * 30,
		60 * 15,
		0));
	private static final String DAILY_PLAYER_CHANGES_COMMAND = "execute as @S at @s run function monumenta:mechanisms/daily_player_changes";
	static ScheduledThreadPoolExecutor mRealTimePool = new ScheduledThreadPoolExecutor(1);
	private static @MonotonicNonNull Runnable mRealTimeRunnable = null;
	private static @MonotonicNonNull BukkitRunnable mCountdownRunnable = null;
	private static int mLastDailyVersion;
	private static int mLastCountdownTarget;

	public static void startTimer(final Plugin plugin) {
		mLastDailyVersion = getDailyVersion();
		mLastCountdownTarget = getNextSecondsTarget();
		if (mRealTimeRunnable == null) {
			scheduleCountdownTick(plugin);
		}
	}

	private static void scheduleCountdownTick(Plugin plugin) {
		long remainingMillis = Math.max(DateUtils.untilNewDay(ChronoUnit.MILLIS) - 1000L * mLastCountdownTarget, 0) + 1;
		if (mCountdownRunnable != null) {
			mCountdownRunnable.cancel();
		}
		mCountdownRunnable = new BukkitRunnable() {
			@Override
			public void run() {
				int nextTarget = getNextSecondsTarget();
				int dailyVersion = getDailyVersion();
				String message;
				boolean targetIsNew = mLastCountdownTarget != nextTarget;
				boolean newDailyVersion = mLastDailyVersion != dailyVersion;
				if (newDailyVersion) {
					message = getNewDayMessage(mLastDailyVersion);
					plugin.getLogger().info("[DailyReset] " + message);
					SeasonalEventManager.reloadPasses(Bukkit.getConsoleSender());
					for (Player player : plugin.getServer().getOnlinePlayers()) {
						handle(plugin, player);
					}
				} else if (targetIsNew) {
					message = getCountdownMessage(mLastCountdownTarget);
					if (message != null) {
						Component component = Component.text(message, NamedTextColor.GOLD, TextDecoration.BOLD);
						plugin.getLogger().info("[DailyReset] " + message);
						for (Player player : plugin.getServer().getOnlinePlayers()) {
							player.sendMessage(component);
						}
					}
				}
				mLastCountdownTarget = nextTarget;
				mLastDailyVersion = dailyVersion;
				scheduleCountdownTick(plugin);
			}
		};
		mRealTimeRunnable = () -> mCountdownRunnable.runTaskLater(plugin, 1);
		mRealTimePool.schedule(mRealTimeRunnable, remainingMillis, TimeUnit.MILLISECONDS);
	}

	private static int getNextSecondsTarget() {
		int secondsRemaining = (int) DateUtils.untilNewDay(ChronoUnit.SECONDS);
		@Nullable Integer nextTarget = COUNTDOWN_SECONDS.floor(secondsRemaining);
		if (nextTarget == null) {
			return 0;
		} else {
			return nextTarget;
		}
	}

	private static int getDailyVersion() {
		// In our specified timezone, how many days we perceive it is since our 1 Jan 1970.
		return (int) DateUtils.getDaysSinceEpoch();
	}

	private static @Nullable String getCountdownMessage(int countdownSeconds) {
		LocalDateTime now = DateUtils.localDateTime();
		long daysLeftInWeek = DateUtils.getDaysLeftInWeeklyVersion(now);

		StringBuilder builder = new StringBuilder("A new ");
		if (daysLeftInWeek == 1) {
			builder.append("week");
		} else {
			builder.append("day");
		}
		if (countdownSeconds <= 0) {
			return null;
		} else if (countdownSeconds == 1) {
			return builder
				.append(" begins in 1 second")
				.toString();
		} else if (countdownSeconds < 60) {
			return builder
				.append(" begins in ")
				.append(countdownSeconds)
				.append(" seconds")
				.toString();
		}
		int minutes = Math.floorDiv(countdownSeconds, 60);
		int seconds = Math.floorMod(countdownSeconds, 60);
		if (minutes == 1) {
			builder.append(" begins in 1 minute");
		} else if (minutes < 60) {
			builder
				.append(" begins in ")
				.append(minutes)
				.append(" minutes");
		}
		if (minutes < 60) {
			if (seconds == 0) {
				return builder.toString();
			} else if (seconds <= 1) {
				return builder
					.append(" 1 second")
					.toString();
			} else {
				return builder
					.append(" ")
					.append(seconds)
					.append(" seconds")
					.toString();
			}
		}
		int hours = Math.floorDiv(minutes, 60);
		minutes = Math.floorMod(minutes, 60);
		if (hours == 1) {
			builder.append(" begins in 1 hour");
		} else {
			builder
				.append(" begins in ")
				.append(hours)
				.append(" hours");
		}
		if (minutes == 0) {
			return builder.toString();
		} else if (minutes == 1) {
			return builder
				.append(" 1 minute")
				.toString();
		} else {
			return builder
				.append(" ")
				.append(minutes)
				.append(" minutes")
				.toString();
		}
	}

	private static String getNewDayMessage(long oldDailyVersion) {
		long oldWeeklyVersion = DateUtils.getWeeklyVersion(oldDailyVersion);
		LocalDateTime now = DateUtils.localDateTime();
		long newWeeklyVersion = DateUtils.getWeeklyVersion(now);
		long daysLeftInWeek = DateUtils.getDaysLeftInWeeklyVersion(now);
		boolean newWeekSoon = daysLeftInWeek == 1;

		StringBuilder builder = new StringBuilder("It's a new ")
			.append(newWeeklyVersion != oldWeeklyVersion ? "week" : "day")
			.append("! A new ")
			.append(newWeekSoon ? "week" : "day")
			.append(" begins in ")
			.append(DateUtils.untilNewDay());
		if (!newWeekSoon) {
			builder.append(", with a new week starting in ")
				.append(daysLeftInWeek)
				.append(" days");
		}
		return builder.toString();
	}

	public static void handle(Plugin plugin, Player player) {
		int dailyVersion = ScoreboardUtils.getScoreboardValue(player, "DailyVersion").orElse(0);
		handle(plugin, player, dailyVersion);
	}

	public static void handle(Plugin plugin, Player player, int dailyVersion) {
		if (player != null) {
			//  Test to see if the player's Daily version is different from the servers.
			if (dailyVersion != getDailyVersion()) {
				//  If so reset some scoreboards and message the player.
				String commandStr = DAILY_PLAYER_CHANGES_COMMAND.replaceAll("@S", player.getName());
				plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), commandStr);

				SeasonalEventManager.handlePlayerDailyChange(player);
				POIManager.handlePlayerDailyChange(player);

				ScoreboardUtils.setScoreboardValue(player, "DailyVersion", getDailyVersion());

				String message = getNewDayMessage(dailyVersion);
				Component component = Component.text(message, NamedTextColor.GOLD, TextDecoration.BOLD);
				player.sendMessage(component);
			}
		}
	}
}
