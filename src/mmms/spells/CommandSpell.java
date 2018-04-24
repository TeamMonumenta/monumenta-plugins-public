package mmms.spells;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class CommandSpell
{
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
