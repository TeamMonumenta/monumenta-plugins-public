package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.managers.TimeWarpManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DateUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LongArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;

public class TimeWarpCommand {
	public static final String COMMAND = "timewarp";
	public static final CommandPermission PERMISSION = CommandPermission.fromString("monumenta.timewarp");

	public static void register() {
		List<Argument<?>> arguments = new ArrayList<>();

		arguments.add(new MultiLiteralArgument("reset"));
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(arguments)
			.executes(TimeWarpCommand::runReset)
			.register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("set"));
		arguments.add(new MultiLiteralArgument("time"));
		arguments.add(new IntegerArgument("hour", 0, 23));
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(arguments)
			.executes(TimeWarpCommand::runTime)
			.register();

		arguments.add(new IntegerArgument("minute", 0, 59));
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(arguments)
			.executes(TimeWarpCommand::runTime)
			.register();

		arguments.add(new IntegerArgument("second", 0, 59));
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(arguments)
			.executes(TimeWarpCommand::runTime)
			.register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("set"));
		arguments.add(new MultiLiteralArgument("date"));
		arguments.add(new IntegerArgument("year", 2000));
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(arguments)
			.executes(TimeWarpCommand::runDateTime)
			.register();

		arguments.add(new IntegerArgument("month", 1, 12));
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(arguments)
			.executes(TimeWarpCommand::runDateTime)
			.register();

		arguments.add(new IntegerArgument("day", 1, 31));
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(arguments)
			.executes(TimeWarpCommand::runDateTime)
			.register();

		arguments.add(new IntegerArgument("hour", 0, 23));
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(arguments)
			.executes(TimeWarpCommand::runDateTime)
			.register();

		arguments.add(new IntegerArgument("minute", 0, 59));
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(arguments)
			.executes(TimeWarpCommand::runDateTime)
			.register();

		arguments.add(new IntegerArgument("second", 0, 59));
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(arguments)
			.executes(TimeWarpCommand::runDateTime)
			.register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("add"));
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
				.withArguments(new MultiLiteralArgument(unit.name().toLowerCase()))
				.executes(TimeWarpCommand::runAdd)
				.register();
		}

		arguments.clear();
		arguments.add(new MultiLiteralArgument("remove"));
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
				.withArguments(new MultiLiteralArgument(unit.name().toLowerCase()))
				.executes(TimeWarpCommand::runRemove)
				.register();
		}
	}

	public static void runReset(CommandSender sender, Object[] args) throws WrapperCommandSyntaxException {
		if (!Plugin.ENABLE_TIME_WARP) {
			throw CommandAPI.failWithString("Time testing is not enabled");
		}

		TimeWarpManager.reset();
		DateVersionCommand.debugDate(sender, DateUtils.localDateTime());
	}

	public static void runTime(CommandSender sender, Object[] args) throws WrapperCommandSyntaxException {
		if (!Plugin.ENABLE_TIME_WARP) {
			throw CommandAPI.failWithString("Time testing is not enabled");
		}

		LocalDateTime targetTime = DateUtils.localDateTime();

		int hour = 0;
		int minute = 0;
		int second = 0;

		if (args.length > 2 && args[2] instanceof Integer arg) {
			hour = arg;
		}

		if (args.length > 3 && args[3] instanceof Integer arg) {
			minute = arg;
		}

		if (args.length > 4 && args[4] instanceof Integer arg) {
			second = arg;
		}

		targetTime = LocalDateTime.of(targetTime.getYear(),
			targetTime.getMonth(),
			targetTime.getDayOfMonth(),
			hour,
			minute,
			second);

		TimeWarpManager.set(targetTime);
		DateVersionCommand.debugDate(sender, DateUtils.localDateTime());
	}

	public static void runDateTime(CommandSender sender, Object[] args) throws WrapperCommandSyntaxException {
		if (!Plugin.ENABLE_TIME_WARP) {
			throw CommandAPI.failWithString("Time testing is not enabled");
		}

		LocalDateTime targetTime = DateUtils.localDateTime();

		int year = targetTime.getYear();
		int month = targetTime.getMonthValue();
		int day = targetTime.getDayOfMonth();
		int hour = targetTime.getHour();
		int minute = targetTime.getMinute();
		int second = targetTime.getSecond();

		if (args.length > 2 && args[2] instanceof Integer arg) {
			year = arg;
		}

		if (args.length > 3 && args[3] instanceof Integer arg) {
			month = arg;
		}

		if (args.length > 4 && args[4] instanceof Integer arg) {
			day = arg;
		}

		if (args.length > 5 && args[5] instanceof Integer arg) {
			hour = arg;
			minute = 0;
			second = 0;
		}

		if (args.length > 6 && args[6] instanceof Integer arg) {
			minute = arg;
		}

		if (args.length > 7 && args[7] instanceof Integer arg) {
			second = arg;
		}

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

	public static void runAdd(CommandSender sender, Object[] args) throws WrapperCommandSyntaxException {
		if (!Plugin.ENABLE_TIME_WARP) {
			throw CommandAPI.failWithString("Time testing is not enabled");
		}

		long amount = 0;
		ChronoUnit unit = ChronoUnit.SECONDS;

		if (args.length > 1 && args[1] instanceof Long arg) {
			amount = arg;
		}

		if (args.length > 2 && args[2] instanceof String arg) {
			unit = ChronoUnit.valueOf(arg.toUpperCase());
		}

		TimeWarpManager.add(amount, unit);
		DateVersionCommand.debugDate(sender, DateUtils.localDateTime());
	}

	public static void runRemove(CommandSender sender, Object[] args) throws WrapperCommandSyntaxException {
		if (!Plugin.ENABLE_TIME_WARP) {
			throw CommandAPI.failWithString("Time testing is not enabled");
		}

		long amount = 0;
		ChronoUnit unit = ChronoUnit.SECONDS;

		if (args.length > 1 && args[1] instanceof Long arg) {
			amount = arg;
		}

		if (args.length > 2 && args[2] instanceof String arg) {
			unit = ChronoUnit.valueOf(arg.toUpperCase());
		}

		TimeWarpManager.add(-amount, unit);
		DateVersionCommand.debugDate(sender, DateUtils.localDateTime());
	}
}
