package pe.project.utils;

import java.lang.reflect.Method;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import pe.project.point.Point;
import pe.project.point.AreaBounds;

public class CommandUtils {
	public static int parseIntFromString(CommandSender sender, Command command, String str) throws Exception {
		int value = 0;

		try{
			value = Integer.parseInt(str);
    	} catch (NumberFormatException e) {
    		sender.sendMessage(ChatColor.RED + "Invalid parameter " + str + ". Must be whole number value between " + Integer.MIN_VALUE + " and " + Integer.MAX_VALUE);
    		sender.sendMessage(ChatColor.RED + "Usage: " + command.getUsage());
    		throw new Exception();
    	}

		return value;
	}

	public static double parseDoubleFromString(CommandSender sender, Command command, String str) throws Exception {
		double value = 0;

		try {
			value = Float.parseFloat(str);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Invalid parameter " + str + ". Must be a value between " + Float.MIN_VALUE + " and " + Float.MAX_VALUE);
    		sender.sendMessage(ChatColor.RED + "Usage: " + command.getUsage());
    		throw new Exception();
		}

		return value;
	}

	public static AreaBounds parseAreaFromString(CommandSender sender, Command command,
	                                             String xStr1, String yStr1, String zStr1,
	                                             String xStr2, String yStr2, String zStr2) throws Exception {
		Point pos1;
		Point pos2;

		try {
			pos1 = parsePointFromString(sender, command, xStr1, yStr1, zStr1, true);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Failed to parse first coordinate");
			throw new Exception();
		}

		try {
			pos2 = parsePointFromString(sender, command, xStr2, yStr2, zStr2, true);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Failed to parse second coordinate");
			throw new Exception();
		}

		pos2.mX += 1;
		pos2.mY += 1;
		pos2.mZ += 1;

		return new AreaBounds("", pos1, pos2);
	}

	public static Point parsePointFromString(CommandSender sender, Command command,
	                                         String xStr, String yStr, String zStr) throws Exception {
		return parsePointFromString(sender, command, xStr, yStr, zStr, false);
	}

	public static Point parsePointFromString(CommandSender sender, Command command,
	                                         String xStr, String yStr, String zStr,
	                                         boolean doSubtractEntityOffset) throws Exception {
		double x = 0;
		double y = 0;
		double z = 0;

		if (xStr.startsWith("~") || yStr.startsWith("~") || zStr.startsWith("~")) {
			Location senderPos = null;

			// Use reflection to find out if the sender's position can be obtained via getLocation()
			// If it can, call it to get those coordinates.
			Method[] methods = sender.getClass().getMethods();
			try {
				for (Method m : methods) {
					if (m.getName().equals("getLocation") && (m.getParameterTypes().length == 0)) {
						// This is for an entity which has a location
						senderPos = (Location)m.invoke(sender, (Object[])null);
						x = senderPos.getX();
						y = senderPos.getY();
						z = senderPos.getZ();
						if (doSubtractEntityOffset) {
							x -= 0.5;
							y -= 0.5;
							z -= 0.5;
						}
						break;
					} else if (m.getName().equals("getBlock") && (m.getParameterTypes().length == 0)) {
						// This is for a block like a command block
						senderPos = ((Block)m.invoke(sender, (Object[])null)).getLocation();
						// Note that the coordinate returned for blocks is the lowest corner
						x = senderPos.getX();
						y = senderPos.getY();
						z = senderPos.getZ();
						break;
					} else if (m.getName().equals("getCallee") && (m.getParameterTypes().length == 0)) {
						// This is for execute commands - CommandSender is a ProxiedCommandSender
						// Get the callee command sender and recurse (only expect to recurse once)
						return parsePointFromString((CommandSender)(m.invoke(sender, (Object[])null)), command, xStr, yStr, zStr, doSubtractEntityOffset);
					}
				}
			} catch (Exception e) {
				// Just in case somehow senderPos was not null and a subsequent call failed
				senderPos = null;
				e.printStackTrace();
			}

			if (senderPos == null) {
				sender.sendMessage(ChatColor.RED + "Failed to get required command sender coordinates");
				throw new Exception();
			}
		}

		try {
			if (xStr.equals("~")) {
				// Nothing to do - coordinate already correct
			} else if (yStr.startsWith("~")) {
				x += parseDoubleFromString(sender, command, xStr.substring(1));
			} else {
				x = parseDoubleFromString(sender, command, xStr);
			}
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Failed to parse x coordinate '" + xStr + "'");
			throw new Exception();
		}

		try {
			if (yStr.equals("~")) {
				// Nothing to do - coordinate already correct
			} else if (yStr.startsWith("~")) {
				y += parseDoubleFromString(sender, command, yStr.substring(1));
			} else {
				y = parseDoubleFromString(sender, command, yStr);
			}
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Failed to parse y coordinate '" + yStr + "'");
			throw new Exception();
		}

		try {
			if (zStr.equals("~")) {
				// Nothing to do - coordinate already correct
			} else if (yStr.startsWith("~")) {
				z += parseDoubleFromString(sender, command, zStr.substring(1));
			} else {
				z = parseDoubleFromString(sender, command, zStr);
			}
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Failed to parse z coordinate '" + zStr + "'");
			throw new Exception();
		}

		return new Point(x, y, z);
	}
}
