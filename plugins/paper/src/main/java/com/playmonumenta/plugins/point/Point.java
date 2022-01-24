package com.playmonumenta.plugins.point;

import com.playmonumenta.plugins.utils.CommandUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import javax.annotation.Nullable;

public class Point {
	public double mX;
	public double mY;
	public double mZ;

	public Point(double x, double y, double z) {
		mX = x;
		mY = y;
		mZ = z;
	}

	public Point(Location loc) {
		mX = loc.getX();
		mY = loc.getY();
		mZ = loc.getZ();
	}

	public static Point fromString(String str) throws Exception {
		return fromString(null, str);
	}

	public static Point fromString(@Nullable CommandSender sender, String str) throws Exception {
		// Remove parenthesis, then split on either , or space or both together
		String[] strArray = str.replaceAll("[()]", "").split("[, ]+");
		if (strArray.length != 3) {
			if (sender != null) {
				sender.sendMessage(ChatColor.RED + "Failed to parse string '" + str +
					                   "' as point - found " + strArray.length + " elements");
			}
			throw new Exception("Failed to parse string '" + str +
				                    "' as point - found " + strArray.length + " elements");
		}
		return fromString(sender, strArray[0], strArray[1], strArray[2], false, null);
	}

	public static Point fromString(String xStr, String yStr, String zStr) throws Exception {
		return fromString(null, xStr, yStr, zStr, false, null);
	}

	public static Point fromString(@Nullable CommandSender sender,
	                               String xStr, String yStr, String zStr) throws Exception {
		return fromString(sender, xStr, yStr, zStr, false, null);
	}

	public static Point fromString(@Nullable CommandSender sender,
	                               String xStr, String yStr, String zStr,
	                               Location senderLoc) throws Exception {
		return fromString(sender, xStr, yStr, zStr, false, senderLoc);
	}

	public static Point fromString(@Nullable CommandSender sender,
	                               String xStr, String yStr, String zStr,
	                               boolean doSubtractEntityOffset) throws Exception {
		return fromString(sender, xStr, yStr, zStr, doSubtractEntityOffset, null);
	}

	public static Point fromString(@Nullable CommandSender sender,
	                               String xStr, String yStr, String zStr,
	                               boolean doSubtractEntityOffset,
	                               @Nullable Location senderLoc) throws Exception {
		double x = 0;
		double y = 0;
		double z = 0;

		if (xStr.startsWith("~") || yStr.startsWith("~") || zStr.startsWith("~")) {
			if (senderLoc == null) {
				senderLoc = CommandUtils.getLocation(sender);
			}
			x = senderLoc.getX();
			y = senderLoc.getY();
			z = senderLoc.getZ();
		}

		x = CommandUtils.parseCoordFromString(sender, x, xStr);
		y = CommandUtils.parseCoordFromString(sender, y, yStr);
		z = CommandUtils.parseCoordFromString(sender, z, zStr);

		return new Point(x, y, z);
	}

	public String toString() {
		return "(" + Double.toString(mX) + ", " +
		       Double.toString(mY) + ", " + Double.toString(mZ) + ")";
	}
}
