package pe.bossfights.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import pe.bossfights.spells.SpellMaskedFrostNova;

public class MaskedFrostNova
{
	private Plugin mPlugin;

	public MaskedFrostNova(Plugin plugin)
	{
		mPlugin = plugin;
	}

	// TODO: Many display message errors...
	public boolean onSpell(CommandSender sender, String[] arg)
	{
		if (arg.length != 3)
		{
			System.out.println(ChatColor.RED + "wrong number of parameters given!\n" + ChatColor.GREEN + "Usage: " + ChatColor.DARK_GREEN + "/mobspell wither_aoe <Radius> <Power> <duration>");
			return false;
		}
		int radius = Integer.parseInt(arg[1]);
		if (radius < 0 || radius > 65535)
		{
			System.out.println(ChatColor.RED + "Radius must be between 0 and 65535");
			return false;
		}
		int time = Integer.parseInt(arg[2]);
		if (time < 0 || time > 500)
		{
			System.out.println(ChatColor.RED + "Power must be between 0 and 500 (ticks)");
			return false;
		}

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
		{
			System.out.println("wither_aoe spell failed");
			return false;
		}

		/* Instantiate and run the spell */
		(new SpellMaskedFrostNova(mPlugin, launcher, radius, time)).run();

		return true;
	}
}
