package mmbf.main;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import mmbf.fights.CAxtal;
import mmbf.fights.Masked_1;
import mmbf.fights.Masked_2;
import mmbf.utils.Utils;

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
		Utils utils = new Utils(plugin);
		Location endLoc = utils.getLocation(utils.calleeEntity(send).getLocation() ,args[1], args[2], args[3]);
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
