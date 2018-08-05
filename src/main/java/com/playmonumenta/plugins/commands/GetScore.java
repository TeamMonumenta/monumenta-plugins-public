package com.playmonumenta.plugins.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class GetScore implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length > 2 || arg3.length == 0) {
			sender.sendMessage(ChatColor.RED + "This command accepts only one or two arguments");
			sender.sendMessage(ChatColor.RED + "Run in one of these ways:");
			sender.sendMessage(ChatColor.RED + "/execute @[...] ~ ~ ~ getscore <scoreboard>");
			sender.sendMessage(ChatColor.RED + "/getscore <name> <scoreboard>");
			sender.sendMessage(ChatColor.RED + "/getscore <scoreboard>");
			return false;
		}

		String target = null;
		String scoreboard = null;
		if (sender instanceof Player) {
			if (arg3.length == 1) {
				target = ((Player)sender).getName();
				scoreboard = arg3[0];
			} else if (arg3.length == 2) {
				target = arg3[0];
				scoreboard = arg3[1];
			}
		} else if (sender instanceof ProxiedCommandSender) {
			CommandSender callee = ((ProxiedCommandSender)sender).getCallee();
			sender = ((ProxiedCommandSender)sender).getCaller();
			if (callee instanceof Player) {
				target = ((Player)callee).getName();
			} else if (callee instanceof Entity) {
				target = ((Entity)callee).getUniqueId().toString();
			} else {
				sender.sendMessage(ChatColor.RED + "Unable to determine execute target");
				return false;
			}

			if (arg3.length == 1) {
				scoreboard = arg3[0];
			} else {
				sender.sendMessage(ChatColor.RED + "When run with /execute, this command accepts only one argument");
				return false;
			}
		} else if (sender instanceof ConsoleCommandSender) {
			if (arg3.length == 2) {
				target = arg3[0];
				scoreboard = arg3[1];
			} else {
				sender.sendMessage(ChatColor.RED + "Missing argument");
				return false;
			}
		}

		if (target == null || scoreboard == null) {
			sender.sendMessage(ChatColor.RED + "???");
			return false;
		}

		int score = ScoreboardUtils.getScoreboardValue(target, scoreboard);
		if (score == -1) {
			sender.sendMessage(ChatColor.RED + "Unable to get score '" + scoreboard + "' for '" + target + "'");
			return false;
		}

		sender.sendMessage(ChatColor.GREEN + target + "'s score for '" + scoreboard + "': " + score);
		return true;
	}
}
