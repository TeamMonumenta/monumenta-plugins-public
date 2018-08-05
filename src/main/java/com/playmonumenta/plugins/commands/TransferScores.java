package com.playmonumenta.plugins.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import pe.project.utils.ScoreboardUtils;

public class TransferScores implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (!(sender instanceof Player) && !(sender instanceof ConsoleCommandSender)) {
			sender.sendMessage(ChatColor.RED + "This command can only be run by a player or the console");
			return false;
		}

		if (arg3.length != 2) {
			sender.sendMessage(ChatColor.RED + "Incorrect number of parameters!");
			return false;
		}

		String from = arg3[0];
		String to = arg3[1];

		try {
			ScoreboardUtils.transferPlayerScores(from, to);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + e.getMessage());
			return false;
		}

		sender.sendMessage(ChatColor.GREEN + "Successfully transfered scoreboard values from '" + from + "' to '" + to + "'");

		return true;
	}
}
