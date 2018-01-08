package pe.project.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;

import pe.project.Plugin;

public class GiveSoulbound implements CommandExecutor {
	Plugin mPlugin;

	public GiveSoulbound(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length < 4) {
			sender.sendMessage(ChatColor.RED + "Invalid number of parameters!");
			return false;
		}

		Player player = null;
		if (sender instanceof Player) {
			player = (Player)sender;
		} else if (sender instanceof ProxiedCommandSender) {
			CommandSender callee = ((ProxiedCommandSender)sender).getCallee();
			if (callee instanceof Player) {
				player = (Player)callee;
			}
		}

		if (player == null) {
            sender.sendMessage(ChatColor.RED + "This command must be run by/on a player!");
			return false;
		}

		String cmdStr = String.join(" ", arg3);

		if (!cmdStr.contains("Lore:[")) {
            sender.sendMessage(ChatColor.RED + "This command can only be used to give items that already have at least one line of lore text");
			return false;
		}

		// Append a line of lore text with the player's name
		cmdStr = cmdStr.replaceAll("(Lore:\\[[^]]*)\\]", "$1,\"* Soulbound to " + player.getName() + " *\"]");

		// Send the command to the console
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "give " + player.getName() + " " + cmdStr);

		/*
		give @a minecraft:stone_sword 1 0 {display:{Name:"§d§lWatcher's Sword",Lore:["* Unique Item *","§fQuis Custodeit Ipsos Custodets?"]},ench:[{id:17,lvl:1},{id:34,lvl:3}]}
		*/

		return true;
	}
}
