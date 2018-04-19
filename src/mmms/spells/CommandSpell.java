package mmms.spells;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import net.md_5.bungee.api.ChatColor;

public class CommandSpell
{

	public CommandSpell()
	{
	}

	Random rand = new Random();

	public boolean onSpell(CommandSender sender, String[] arg)
	{
		if (arg.length <= 1)
		{
			System.out.println(ChatColor.RED + "wrong number of parameters given!\n");
			return (true);
		}
		arg[0] = "";
		String command = String.join(" ", arg).trim();
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
		return true;
	}
}
