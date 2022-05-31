package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.ObjectiveArgument;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class DateVersionCommand {
	private enum VersionField {
		DAILY_VERSION("DailyVersion"),
		WEEKLY_VERSION("WeeklyVersion"),
		DAYS_INTO_WEEK("DaysIntoWeek"),
		DAYS_LEFT_IN_WEEK("DaysLeftInWeek"),
		WEEK_START_DATE("WeekStartDate"),
		WEEK_END_DATE("WeekEndDate"),
		NEXT_WEEK_DATE("NextWeekDate"),
		SECONDS_TO_NEXT_DAY("SecondsToNextDay"),
		SECONDS_TO_NEXT_WEEK("SecondsToNextWeek"),
		DEBUG("Debug");

		final String mArgument;

		VersionField(String argument) {
			mArgument = argument;
		}

		public String getArgument() {
			return mArgument;
		}
	}

	public static void register() {
		for (VersionField versionField : VersionField.values()) {
			if (versionField.equals(VersionField.SECONDS_TO_NEXT_DAY) ||
				versionField.equals(VersionField.SECONDS_TO_NEXT_WEEK)) {
				new CommandAPICommand("dateversion")
					.withPermission(CommandPermission.fromString("monumenta.dateversion"))
					.withSubcommand(new CommandAPICommand(versionField.getArgument())
						.executes((sender, args) -> {
							LocalDateTime localDateTime = DateUtils.localDateTime();

							return getDateVersionResult(sender, localDateTime, versionField);
						})
					).register();
			} else {
				new CommandAPICommand("dateversion")
					.withPermission(CommandPermission.fromString("monumenta.dateversion"))
					.withSubcommand(new CommandAPICommand(versionField.getArgument())
						.withSubcommand(new CommandAPICommand("Now")
							.executes((sender, args) -> {
								LocalDateTime localDateTime = DateUtils.localDateTime();

								return getDateVersionResult(sender, localDateTime, versionField);
							}))
						.withSubcommand(new CommandAPICommand("Date")
							.withArguments(new IntegerArgument("Year", 1900, 2100))
							.withArguments(new IntegerArgument("Month", 1, 12))
							.withArguments(new IntegerArgument("DayOfMonth", 1, 31))
							.executes((sender, args) -> {
								int year = (Integer) args[0];
								int month = (Integer) args[1];
								int dayOfMonth = (Integer) args[2];
								LocalDateTime localDateTime = DateUtils.localDateTime(year, month, dayOfMonth);

								return getDateVersionResult(sender, localDateTime, versionField);
							}))
						.withSubcommand(new CommandAPICommand("DailyVersionScore")
							.withArguments(new ObjectiveArgument("DailyVersionScore"))
							.executesEntity((entity, args) -> {
								String objective = (String) args[0];
								int dailyVersion = ScoreboardUtils.getScoreboardValue(entity, objective).orElse(0);
								LocalDateTime localDateTime = DateUtils.localDateTime(dailyVersion);

								return getDateVersionResult(entity, localDateTime, versionField);
							}))
						.withSubcommand(new CommandAPICommand("DailyVersionConst")
							.withArguments(new IntegerArgument("DailyVersionConst"))
							.executes((sender, args) -> {
								int dailyVersion = (Integer) args[0];
								LocalDateTime localDateTime = DateUtils.localDateTime(dailyVersion);

								return getDateVersionResult(sender, localDateTime, versionField);
							}))
					).register();
			}
		}
	}

	private static int getDateVersionResult(CommandSender sender, LocalDateTime localDateTime, VersionField versionField) {
		switch (versionField) {
			case DAILY_VERSION:
				return (int) DateUtils.getDaysSinceEpoch(localDateTime);
			case WEEKLY_VERSION:
				return (int) DateUtils.getWeeklyVersion(localDateTime);
			case DAYS_INTO_WEEK:
				return (int) DateUtils.getDaysIntoWeeklyVersion(localDateTime);
			case DAYS_LEFT_IN_WEEK:
				return (int) DateUtils.getDaysLeftInWeeklyVersion(localDateTime);
			case WEEK_START_DATE:
				return (int) DateUtils.getWeeklyVersionStartDate(localDateTime);
			case WEEK_END_DATE:
				return (int) DateUtils.getWeeklyVersionEndDate(localDateTime);
			case NEXT_WEEK_DATE:
				return (int) DateUtils.getNextWeeklyVersionStartDate(localDateTime);
			case SECONDS_TO_NEXT_DAY:
				return (int) DateUtils.untilNewDay(ChronoUnit.SECONDS);
			case SECONDS_TO_NEXT_WEEK:
				return (int) DateUtils.untilNewWeek(ChronoUnit.SECONDS);
			default:
				debugDate(sender, localDateTime);
				return 1;
		}
	}

	private static void debugDate(CommandSender sender, LocalDateTime localDateTime) {
		sender.sendMessage(Component.text("Current Tick: " + Bukkit.getCurrentTick()));
		sender.sendMessage(Component.text("Now UntilNextDay: " + DateUtils.untilNewDay()));
		sender.sendMessage(Component.text("Now UntilNextWeek: " + DateUtils.untilNewWeek()));
		sender.sendMessage(Component.empty());

		sender.sendMessage(Component.text("For " + localDateTime + ":"));
		sender.sendMessage(Component.text("DailyVersion: " + DateUtils.getDaysSinceEpoch(localDateTime)));
		sender.sendMessage(Component.text("WeeklyVersion: " + DateUtils.getWeeklyVersion(localDateTime)));
		sender.sendMessage(Component.text("DaysIntoWeeklyVersion: " + DateUtils.getDaysIntoWeeklyVersion(localDateTime)));
		sender.sendMessage(Component.text("DaysLeftInWeeklyVersion: " + DateUtils.getDaysLeftInWeeklyVersion(localDateTime)));
		sender.sendMessage(Component.text("WeeklyVersionStartDate: " + DateUtils.getWeeklyVersionStartDate(localDateTime)));
		sender.sendMessage(Component.text("WeeklyVersionEndDate: " + DateUtils.getWeeklyVersionEndDate(localDateTime)));
		sender.sendMessage(Component.text("NextWeeklyVersionStartDate: " + DateUtils.getNextWeeklyVersionStartDate(localDateTime)));
	}
}
