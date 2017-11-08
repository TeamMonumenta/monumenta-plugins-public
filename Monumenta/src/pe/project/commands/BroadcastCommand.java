package pe.project.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;

import org.bukkit.ChatColor;
import pe.project.Main;
import pe.project.utils.NetworkUtils;

public class BroadcastCommand implements CommandExecutor {
	Main mMain;

	public BroadcastCommand(Main main) {
		mMain = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length == 0) {
			sender.sendMessage(ChatColor.RED + "Invalid number of parameters!");
			return false;
		}

		/* Only allow reasonable commands */
		switch (arg3[0]) {
			case "whitelist":
			case "ban":
			case "ban-ip":
			case "pardon":
			case "pardon-ip":
			case "op":
			case "deop":

			case "say":
			case "msg":
			case "tell":
			case "tellraw":
			case "title":

			case "function":
			case "difficulty":
			case "scoreboard":
			case "setblock":
				break;
			default:
				sender.sendMessage(ChatColor.RED + "The command '" + arg3[0] + "' is not supported!");
				return false;
		}

		String playerName = null;
		if (sender instanceof Player) {
			playerName = sender.getName();
		} else if (sender instanceof ProxiedCommandSender) {
			CommandSender callee = ((ProxiedCommandSender)sender).getCallee();
			if (callee instanceof Player) {
				playerName = callee.getName();
			}
		}

		String commandStr = "";
		for (String str: arg3) {
			if (commandStr != "") {
				commandStr += " ";
			}

			// If possible, replace @s with player's name
			if (str.contains("@s")) {
				if (playerName == null) {
					sender.sendMessage(ChatColor.RED + "Failed to resolve @s argument!");
				} else {
					str = str.replace("@s", playerName);
				}
			}

			// Replace the special @A token with @a
			// (so Minecraft isn't allowed to resolve it preemptively)
			str = str.replace("@A", "@a");

			commandStr += str;
		}

		sender.sendMessage(ChatColor.GOLD + "Broadcasting command '" + commandStr + "' to all servers!");
		try {
			NetworkUtils.broadcastCommand(mMain, commandStr);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Broadcasting command failed");
		}

		return true;
	}
}
