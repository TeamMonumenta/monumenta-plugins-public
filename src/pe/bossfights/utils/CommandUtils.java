package pe.bossfights.utils;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;

public class CommandUtils
{
	public static class ArgumentException extends Exception
	{
		private static final long serialVersionUID = 1L;
		public ArgumentException(String message)
		{
			super(message);
		}
	}

	public static void assertArgCount(String[] arg, int expectedCount) throws ArgumentException
	{
		if ((arg.length - 1) != expectedCount)
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
}
