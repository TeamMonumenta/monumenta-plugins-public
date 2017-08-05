package pe.project.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import pe.project.point.Point;

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
	
	public static Point parsePointFromString(CommandSender sender, Command command, String xStr, String yStr, String zStr) throws Exception {
		double x;
		try {
			x = parseDoubleFromString(sender, command, xStr);
		} catch (Exception e) {
			throw new Exception();
		}
		
		double y;
		try {
			y = parseDoubleFromString(sender, command, yStr);
		} catch (Exception e) {
			throw new Exception();
		}
		
		double z;
		try {
			z = parseDoubleFromString(sender, command, zStr);
		} catch (Exception e) {
			throw new Exception();
		}
		
		return new Point(x, y, z);
	}
}
