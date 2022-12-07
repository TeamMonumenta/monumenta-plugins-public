package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.point.AreaBounds;
import com.playmonumenta.plugins.point.Point;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class CommandUtils {

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

	public static int parseIntFromString(@Nullable CommandSender sender, String str) throws Exception {
		int value = 0;

		try {
			value = Integer.parseInt(str);
		} catch (NumberFormatException e) {
			if (sender != null) {
				error(sender, "Invalid parameter " + str + ". Must be whole number value between " + Integer.MIN_VALUE + " and " + Integer.MAX_VALUE);
			}
			throw new Exception(e);
		}

		return value;
	}

	public static double parseDoubleFromString(@Nullable CommandSender sender, String str) throws Exception {
		double value = 0;

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

	public static AreaBounds parseAreaFromString(@Nullable CommandSender sender,
	                                             String xStr1, String yStr1, String zStr1,
	                                             String xStr2, String yStr2, String zStr2) throws Exception {
		Point pos1;
		Point pos2;

		try {
			pos1 = Point.fromString(sender, xStr1, yStr1, zStr1, true);
		} catch (Exception e) {
			if (sender != null) {
				error(sender, "Failed to parse first coordinate");
			}
			throw new Exception(e);
		}

		try {
			pos2 = Point.fromString(sender, xStr2, yStr2, zStr2, true);
		} catch (Exception e) {
			if (sender != null) {
				error(sender, "Failed to parse second coordinate");
			}
			throw new Exception(e);
		}

		pos2.mX += 1;
		pos2.mY += 1;
		pos2.mZ += 1;

		return new AreaBounds(pos1, pos2);
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
			sender.sendMessage(ChatColor.RED + msg);
		} else {
			sender.sendMessage(msg);
		}
	}

	public static void runCommandViaConsole(String cmd) {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
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
		CommandAPI.fail("This command must be run by/as a player");
		throw new RuntimeException(); // This can never happen but is required by the compiler
	}
}
