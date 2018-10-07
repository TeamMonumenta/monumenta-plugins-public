package com.playmonumenta.plugins.rawcommands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class GenericCommand {
	protected static void error(CommandSender sender, String msg) {
		sender.sendMessage(ChatColor.RED + msg);
	}
}
