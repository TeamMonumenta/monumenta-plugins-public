package com.playmonumenta.plugins.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;

public class RemoveTags implements CommandExecutor {
	Plugin mPlugin;

	public RemoveTags(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length > 0) {
			sender.sendMessage(ChatColor.RED + "No parameters are needed for this function!");
			return false;
		}

		if (!(sender instanceof ProxiedCommandSender)) {
			sender.sendMessage(ChatColor.RED + "This command can only be run on an entity/player via /execute!");
			return false;
		}

		CommandSender callee = ((ProxiedCommandSender)sender).getCallee();
		if (!(callee instanceof Entity)) {
			sender.sendMessage(ChatColor.RED + "The target of this command must be an entity!");
			return false;
		}

		Entity target = (Entity)callee;

		target.getScoreboardTags().clear();

		if (callee instanceof Player) {
			sender.sendMessage("Cleared all tags from player '" + ((Player)callee).getName() + "'");
		} else {
			sender.sendMessage("Cleared all tags from entity '" + target.getUniqueId() + "'");
		}

		return true;
	}
}
