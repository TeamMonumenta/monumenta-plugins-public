package mmbf.main;

import mmbf.fights.CAxtal;
import mmbf.fights.Masked_1;
import mmbf.fights.Masked_2;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.Location;

import pe.bossfights.utils.Utils;
import pe.bossfights.utils.Utils.ArgumentException;
import net.md_5.bungee.api.ChatColor;

public class BossFight implements CommandExecutor
{
	Main plugin;

	public BossFight(Main pl)
	{
		plugin = pl;
	}

	@Override
	public boolean onCommand(CommandSender send, Command command, String label, String[] args)
	{
		if (args.length < 4)
			return (false);
		Location endLoc;
		try
		{
			endLoc = Utils.getLocation(Utils.calleeEntity(send).getLocation(), args[1], args[2], args[3]);
		}
		catch (ArgumentException ex)
		{
			send.sendMessage(ChatColor.RED + ex.getMessage());
			return false;
		}
		String input = args[0].toLowerCase();

		switch (input)
		{
		case "caxtal":
			(new CAxtal(plugin)).spawn(send, endLoc);
			break;
		case "masked_1":
			(new Masked_1(plugin)).spawn(send, endLoc);
			break;
		case "masked_2":
			(new Masked_2(plugin)).spawn(send, endLoc);
			break;
		}
		return true;
	}
}
