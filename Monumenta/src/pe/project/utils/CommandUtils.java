package pe.project.utils;

import java.lang.reflect.Method;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import pe.project.point.Point;
import pe.project.point.AreaBounds;
import pe.project.utils.LocationUtils.LocationType;

public class CommandUtils {

	/**
	 * Gets a CommandSender's location (player, command block, /execute, etc.)
	 *
	 * @return sender's location or raises an exception
	 */
	public static Location getLocation(CommandSender sender) throws Exception {
		return getLocation(sender, false);
	}

	// TODO: Reflection is cute and all, but instanceof would be way simpler/better
	public static Location getLocation(CommandSender sender, boolean doSubtractEntityOffset) throws Exception {
		if (sender == null) {
			throw new Exception("sender is null!");
		}
		Location senderLoc = null;

		// Use reflection to find out if the sender's position can be obtained via getLocation()
		// If it can, call it to get those coordinates.
		Method[] methods = sender.getClass().getMethods();
		try {
			for (Method m : methods) {
				if (m.getName().equals("getLocation") && (m.getParameterTypes().length == 0)) {
					// This is for an entity which has a location
					senderLoc = (Location)m.invoke(sender, (Object[])null);
					if (doSubtractEntityOffset) {
						senderLoc.subtract(0.5, 0.5, 0.5);
					}
					break;
				} else if (m.getName().equals("getBlock") && (m.getParameterTypes().length == 0)) {
					// This is for a block like a command block
					// Note that the coordinate returned for blocks is the lowest corner
					senderLoc = ((Block)m.invoke(sender, (Object[])null)).getLocation();
					break;
				} else if (m.getName().equals("getCallee") && (m.getParameterTypes().length == 0)) {
					// This is for execute commands - CommandSender is a ProxiedCommandSender
					// Get the callee command sender and recurse (only expect to recurse once)
					return getLocation(((CommandSender)(m.invoke(sender, (Object[])null))), doSubtractEntityOffset);
				}
			}
		} catch (Exception e) {
			// Just in case somehow senderLoc was not null and a subsequent call failed
			senderLoc = null;
			e.printStackTrace();
		}

		if (senderLoc == null) {
			sender.sendMessage(ChatColor.RED + "Failed to get required command sender coordinates");
			throw new Exception("Failed to get required command sender coordinates");
		}

		return senderLoc;
	}

	public static int parseIntFromString(CommandSender sender, String str) throws Exception {
		int value = 0;

		try{
			value = Integer.parseInt(str);
    	} catch (NumberFormatException e) {
    		if (sender != null) {
				sender.sendMessage(ChatColor.RED + "Invalid parameter " + str + ". Must be whole number value between " + Integer.MIN_VALUE + " and " + Integer.MAX_VALUE);
			}
    		throw new Exception(e);
    	}

		return value;
	}

	public static double parseDoubleFromString(CommandSender sender, String str) throws Exception {
		double value = 0;

		try {
			value = Float.parseFloat(str);
		} catch (Exception e) {
			if (sender != null) {
				sender.sendMessage(ChatColor.RED + "Invalid parameter " + str + ". Must be a value between " + Float.MIN_VALUE + " and " + Float.MAX_VALUE);
			}
    		throw new Exception(e);
		}

		return value;
	}

	public static AreaBounds parseAreaFromString(CommandSender sender,
	                                             String xStr1, String yStr1, String zStr1,
	                                             String xStr2, String yStr2, String zStr2) throws Exception {
		Point pos1;
		Point pos2;

		try {
			pos1 = Point.fromString(sender, xStr1, yStr1, zStr1, true);
		} catch (Exception e) {
			if (sender != null) {
				sender.sendMessage(ChatColor.RED + "Failed to parse first coordinate");
			}
			throw new Exception(e);
		}

		try {
			pos2 = Point.fromString(sender, xStr2, yStr2, zStr2, true);
		} catch (Exception e) {
			if (sender != null) {
				sender.sendMessage(ChatColor.RED + "Failed to parse second coordinate");
			}
			throw new Exception(e);
		}

		pos2.mX += 1;
		pos2.mY += 1;
		pos2.mZ += 1;

		return new AreaBounds("", LocationType.None, pos1, pos2);
	}

	public static double parseCoordFromString(CommandSender sender,
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
				sender.sendMessage(ChatColor.RED + "Failed to parse coordinate '" + str + "'");
			}
			throw new Exception(e);
		}
	}
}
