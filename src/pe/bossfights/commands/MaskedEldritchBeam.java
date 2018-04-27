package pe.bossfights.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import pe.bossfights.spells.SpellMaskedEldritchBeam;

public class MaskedEldritchBeam
{
	private Plugin mPlugin;

	public MaskedEldritchBeam(Plugin plugin)
	{
		mPlugin = plugin;
	}

	// TODO: Many display message errors...
	public boolean onSpell(CommandSender sender, String[] arg)
	{
		if (arg.length != 1)
		{
			System.out.println(ChatColor.RED + "wrong number of parameters given!\n" + ChatColor.GREEN + "Usage: " + ChatColor.DARK_GREEN + "/mobspell Tnt_Throw <Count> <Cooldown>");
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
		(new SpellMaskedEldritchBeam(mPlugin, launcher)).run();

		return true;
	}
}
