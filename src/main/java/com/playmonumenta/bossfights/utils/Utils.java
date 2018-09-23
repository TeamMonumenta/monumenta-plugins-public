package com.playmonumenta.bossfights.utils;

import com.playmonumenta.bossfights.spells.SpellBaseCharge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import org.bukkit.Location;

public class Utils
{
	public static class ArgumentException extends Exception
	{
		private static final long serialVersionUID = 1L;
		public ArgumentException(String message)
		{
			super(message);
		}
	}

	public static List<Player> playersInRange(Location loc, double range)
	{
		List<Player> out = new ArrayList<Player>();

		for (Player player : Bukkit.getServer().getOnlinePlayers())
		{
			if (player.getLocation().distance(loc) < range &&
			    player.getGameMode() != GameMode.SPECTATOR &&
			    player.getHealth() > 0)
				out.add(player);
		}
		return out;
	}

	public static Location getLocation(Location origin, String sx, String sy, String sz)
	{
		Location out = new Location(origin.getWorld(), 0, 0, 0);
		if (sx.charAt(0) == '~')
		{
			if (sx.length() > 1)
				sx = sx.substring(1);
			else
				sx = Integer.toString(0);
			out.setX(origin.getX() + Double.parseDouble(sx));
		}
		else
			out.setX(Double.parseDouble(sx));
		if (sy.charAt(0) == '~')
		{
			if (sy.length() > 1)
				sy = sy.substring(1);
			else
				sy = Integer.toString(0);
			out.setY(origin.getY() + Double.parseDouble(sy));
		}
		else
			out.setY(Double.parseDouble(sy));
		if (sz.charAt(0) == '~')
		{
			if (sz.length() > 1)
				sz = sz.substring(1);
			else
				sz = Integer.toString(0);
			out.setZ(origin.getZ() + Double.parseDouble(sz));
		}
		else
			out.setZ(Double.parseDouble(sz));
		return out;
	}

	public static void assertArgCount(String[] arg, int expectedCount) throws ArgumentException
	{
		if (arg.length - 1 != expectedCount)
			throw new ArgumentException("Expected " + Integer.toString(expectedCount) + " arguments, got " + Integer.toString(arg.length - 1));
	}

	public static int parseInt(String arg, int min, int max) throws ArgumentException
	{
		int val;
		try {
			val = Integer.parseInt(arg);
		}
		catch (NumberFormatException e)
		{
			throw new ArgumentException("Unable to parse '" + arg + "' as int");
		}

		if (val < min || val > max)
			throw new ArgumentException("Expected integer in range [" + Integer.toString(min) + "," + Integer.toString(max) + "], got " + arg);
		return val;
	}

	public static Entity calleeEntity(CommandSender sender) throws ArgumentException
	{
		Entity launcher = null;
		if (sender instanceof Entity)
			launcher = (Entity)sender;
		else if (sender instanceof ProxiedCommandSender)
		{
			CommandSender callee = ((ProxiedCommandSender)sender).getCallee();
			if (callee instanceof Entity)
				launcher = (Entity)callee;
		}
		if (launcher == null)
			throw new ArgumentException("Unable to determine target entity");
		return launcher;
	}

	/* Command should use @s for targeting selector */
	public static String getExecuteCommandOnNearbyPlayers(Location loc, int radius, String command)
	{
		String executeCmd = "execute @a[x=" + (int)loc.getX() +
		                    ",y=" + (int)loc.getY() +
		                    ",z=" + (int)loc.getZ() +
		                    ",r=" + radius + "] ~ ~ ~ ";
		return executeCmd + command;
	}

	public static void executeCommandOnNearbyPlayers(Location loc, int radius, String command)
	{
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
		                                   getExecuteCommandOnNearbyPlayers(loc, radius, command));
	}

	/*
	 * Uses the charge mechanic to detect if a player has line of sight to a location (usually boss.getEyeLocation())
	 */
	public static boolean hasLineOfSight(Player player, LivingEntity target) {
		return SpellBaseCharge.doCharge(player, target, player.getEyeLocation(), Arrays.asList(player), null, null, null, null, false);
	}
}
