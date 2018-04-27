package pe.bossfights.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import pe.bossfights.spells.SpellMaskedShadowGlade;

public class MaskedShadowGlade
{
	private Plugin mPlugin;

	public MaskedShadowGlade(Plugin plugin)
	{
		mPlugin = plugin;
	}

	// TODO: Many display message errors...
	public boolean onSpell(CommandSender sender, String[] arg)
	{
		if (arg.length != 2)
		{
			System.out.println(ChatColor.RED + "wrong number of parameters given!\n" + ChatColor.GREEN + "Usage: " + ChatColor.DARK_GREEN + "/mobspell Melee_Minions_1 <Count> <Scope> <Repeats>");
			return false;
		}
		int count = Integer.parseInt(arg[1]);
		if (count < 0 || count > 4)
		{
			System.out.println(ChatColor.RED + "Count must be between 0 and 4");
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
		(new SpellMaskedShadowGlade(mPlugin, launcher.getLocation(), count)).run();

		return true;
	}
}
