package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.managers.TimeWarpManager;
import com.playmonumenta.plugins.utils.DateUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.LongArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.command.CommandSender;

public class TimeWarpCommand {
	public static final String COMMAND = "timewarp";
	public static final CommandPermission PERMISSION = CommandPermission.fromString("monumenta.timewarp");

	public static void register() {
		List<Argument<?>> arguments = new ArrayList<>();

		arguments.add(new LiteralArgument("reset"));
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(arguments)
			.executes(TimeWarpCommand::runReset)
			.register();

		arguments.clear();
		arguments.add(new LiteralArgument("set"));
		arguments.add(new LiteralArgument("time"));
		arguments.add(new IntegerArgument("hour", 0, 23));
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(arguments)
			.withOptionalArguments(
				new IntegerArgument("minute", 0, 59),
				new IntegerArgument("second", 0, 59)
			)
			.executes(TimeWarpCommand::runTime)
			.register();

		arguments.clear();
		arguments.add(new LiteralArgument("set"));
		arguments.add(new LiteralArgument("date"));
		arguments.add(new IntegerArgument("year", 2000));
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(arguments)
			.withOptionalArguments(
				new IntegerArgument("month", 1, 12),
				new IntegerArgument("day", 1, 31),
				new IntegerArgument("hour", 0, 23),
				new IntegerArgument("minute", 0, 59),
				new IntegerArgument("second", 0, 59)
			)
			.executes(TimeWarpCommand::runDateTime)
			.register();

		arguments.clear();
		arguments.add(new LiteralArgument("add"));
		arguments.add(new LongArgument("amount"));
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(arguments)
			.executes(TimeWarpCommand::runAdd)
			.register();

		for (ChronoUnit unit : ChronoUnit.values()) {
			if (ChronoUnit.SECONDS.compareTo(unit) > 0) {
				continue;
			}

			new CommandAPICommand(COMMAND)
				.withPermission(PERMISSION)
				.withArguments(arguments)
				.withArguments(new MultiLiteralArgument("unit", unit.name().toLowerCase(Locale.getDefault())))
				.executes(TimeWarpCommand::runAdd)
				.register();
		}

		arguments.clear();
		arguments.add(new LiteralArgument("remove"));
		arguments.add(new LongArgument("amount"));
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(arguments)
			.executes(TimeWarpCommand::runRemove)
			.register();

		for (ChronoUnit unit : ChronoUnit.values()) {
			if (ChronoUnit.SECONDS.compareTo(unit) > 0) {
				continue;
			}

			new CommandAPICommand(COMMAND)
				.withPermission(PERMISSION)
				.withArguments(arguments)
				.withArguments(new MultiLiteralArgument("unit", unit.name().toLowerCase(Locale.getDefault())))
				.executes(TimeWarpCommand::runRemove)
				.register();
		}
	}

	public static void runReset(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
		if (!Plugin.ENABLE_TIME_WARP) {
			throw CommandAPI.failWithString("Time testing is not enabled");
		}

		TimeWarpManager.reset();
		DateVersionCommand.debugDate(sender, DateUtils.localDateTime());
	}

	public static void runTime(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
		if (!Plugin.ENABLE_TIME_WARP) {
			throw CommandAPI.failWithString("Time testing is not enabled");
		}

		LocalDateTime targetTime = DateUtils.localDateTime();

		int hour = args.getOrDefaultUnchecked("hour", 0);
		int minute = args.getOrDefaultUnchecked("minute", 0);
		int second = args.getOrDefaultUnchecked("second", 0);

		targetTime = LocalDateTime.of(targetTime.getYear(),
			targetTime.getMonth(),
			targetTime.getDayOfMonth(),
			hour,
			minute,
			second);

		TimeWarpManager.set(targetTime);
		DateVersionCommand.debugDate(sender, DateUtils.localDateTime());
	}

	public static void runDateTime(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
		if (!Plugin.ENABLE_TIME_WARP) {
			throw CommandAPI.failWithString("Time testing is not enabled");
		}

		LocalDateTime targetTime = DateUtils.localDateTime();

		int year = args.getOrDefaultUnchecked("year", targetTime.getYear());
		int month = args.getOrDefaultUnchecked("month", targetTime.getMonthValue());
		int day = args.getOrDefaultUnchecked("day", targetTime.getDayOfMonth());
		Optional<Integer> hourOptional = args.getOptionalUnchecked("hour");
		int hour = hourOptional.orElse(targetTime.getHour());
		int minute = args.getOrDefaultUnchecked("minute", hourOptional.isPresent() ? 0 : targetTime.getMinute());
		int second = args.getOrDefaultUnchecked("second", hourOptional.isPresent() ? 0 : targetTime.getSecond());

		try {
			targetTime = LocalDateTime.of(year,
				month,
				day,
				hour,
				minute,
				second);
		} catch (DateTimeException ex) {
			throw CommandAPI.failWithString("Could not change time: " + ex);
		}

		TimeWarpManager.set(targetTime);
		DateVersionCommand.debugDate(sender, DateUtils.localDateTime());
	}

	public static void runAdd(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
		if (!Plugin.ENABLE_TIME_WARP) {
			throw CommandAPI.failWithString("Time testing is not enabled");
		}

		long amount = args.getOrDefaultUnchecked("amount", 0L);
		String unitName = args.getUnchecked("unit");
		ChronoUnit unit = unitName == null ? ChronoUnit.SECONDS : ChronoUnit.valueOf(unitName.toUpperCase(Locale.getDefault()));

		TimeWarpManager.add(amount, unit);
		DateVersionCommand.debugDate(sender, DateUtils.localDateTime());
	}

	public static void runRemove(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
		if (!Plugin.ENABLE_TIME_WARP) {
			throw CommandAPI.failWithString("Time testing is not enabled");
		}

		long amount = args.getOrDefaultUnchecked("amount", 0L);
		String unitName = args.getUnchecked("unit");
		ChronoUnit unit = unitName == null ? ChronoUnit.SECONDS : ChronoUnit.valueOf(unitName.toUpperCase(Locale.getDefault()));

		TimeWarpManager.add(-amount, unit);
		DateVersionCommand.debugDate(sender, DateUtils.localDateTime());
	}
}
