package mmbf.utils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class Utils
{
	public static Entity calleeEntity(CommandSender sender)
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
		return (launcher);
	}

	public static List<Player> playersInRange(Location loc, double range)
	{
		List<Player> out = new ArrayList<Player>();

		for (Player player : Bukkit.getServer().getOnlinePlayers())
		{
			if (player.getLocation().distance(loc) < range && player.getGameMode() == GameMode.SURVIVAL)
				out.add(player);
		}
		return (out);
	}

	public static boolean errorMsg(String str)
	{
		Bukkit.broadcastMessage(str);
		return false;
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
		return (out);
	}
}
