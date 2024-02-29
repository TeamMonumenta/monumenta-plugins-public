package com.playmonumenta.plugins.utils;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class CommandUtils {

	private static final Pattern RE_ALLOWED_WITHOUT_QUOTES = Pattern.compile("[0-9A-Za-z_.+-]+");

	public static CommandSender getCallee(CommandSender sender) {
		if (sender instanceof ProxiedCommandSender) {
			return ((ProxiedCommandSender)sender).getCallee();
		}
		return sender;
	}

	/**
	 * Gets a CommandSender's location (player, command block, /execute, etc.)
	 *
	 * @return sender's location or raises an exception
	 */
	public static Location getLocation(@Nullable CommandSender sender) throws Exception {
		return getLocation(sender, false);
	}

	public static Location getLocation(@Nullable CommandSender sender, boolean doSubtractEntityOffset) throws Exception {
		if (sender == null) {
			throw new Exception("sender is null!");
		} else if (sender instanceof Entity) {
			Location senderLoc = ((Entity) sender).getLocation();
			if (doSubtractEntityOffset) {
				senderLoc.subtract(0.5, 0.5, 0.5);
			}
			return senderLoc;
		} else if (sender instanceof BlockCommandSender) {
			return ((BlockCommandSender) sender).getBlock().getLocation();
		} else if (sender instanceof ProxiedCommandSender) {
			return getLocation(((ProxiedCommandSender)sender).getCallee(), doSubtractEntityOffset);
		} else {
			throw new Exception("Failed to get required command sender coordinates");
		}
	}

	public static double parseDoubleFromString(@Nullable CommandSender sender, String str) throws Exception {
		double value;

		try {
			value = Float.parseFloat(str);
		} catch (Exception e) {
			if (sender != null) {
				error(sender, "Invalid parameter " + str + ". Must be a value between " + Float.MIN_VALUE + " and " + Float.MAX_VALUE);
			}
			throw new Exception(e);
		}

		return value;
	}

	public static double parseCoordFromString(@Nullable CommandSender sender,
	                                          double senderPos, String str) throws Exception {
		try {
			if (str.equals("~")) {
				return senderPos;
			} else if (str.startsWith("~")) {
				return senderPos + parseDoubleFromString(sender, str.substring(1));
			} else {
				return parseDoubleFromString(sender, str);
			}
		} catch (Exception e) {
			if (sender != null) {
				error(sender, "Failed to parse coordinate '" + str + "'");
			}
			throw new Exception(e);
		}
	}

	public static void error(CommandSender sender, String msg) {
		if ((sender instanceof Player)
			|| ((sender instanceof ProxiedCommandSender) && (((ProxiedCommandSender)sender).getCaller() instanceof Player))) {
			sender.sendMessage(Component.text(msg, NamedTextColor.RED));
		} else {
			sender.sendMessage(msg);
		}
	}

	/**
	 * Returns the sender as Player, if that sender is a player instance, or a proxied player.
	 * Fails with an error message if not executed by/as a player.
	 */
	public static Player getPlayerFromSender(CommandSender sender) throws WrapperCommandSyntaxException {
		if (sender instanceof Player) {
			return ((Player) sender);
		} else if (sender instanceof ProxiedCommandSender) {
			CommandSender callee = ((ProxiedCommandSender) sender).getCallee();
			if (callee instanceof Player) {
				return ((Player) callee);
			}
		}
		throw CommandAPI.failWithString("This command must be run by/as a player");
	}

	public static boolean requiresQuotes(String arg) {
		if (arg == null) {
			return true;
		}
		return !RE_ALLOWED_WITHOUT_QUOTES.matcher(arg).matches();
	}

	public static @Nullable String quoteIfNeeded(@Nullable String arg) {
		if (arg == null) {
			return null;
		}
		if (requiresQuotes(arg)) {
			return "\"" + arg.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
		} else {
			return arg;
		}
	}

}
