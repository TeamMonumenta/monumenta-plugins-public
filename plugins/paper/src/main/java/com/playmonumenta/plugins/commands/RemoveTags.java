package com.playmonumenta.plugins.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class RemoveTags extends GenericCommand {
	public static void register() {
		registerEntityCommand("removetags", "monumenta.command.removetags", RemoveTags::run);
	}

	private static void run(CommandSender sender, Entity entity) {
		entity.getScoreboardTags().clear();

		if (entity instanceof Player) {
			sender.sendMessage(ChatColor.GOLD + "Cleared all tags from player '" + ((Player)entity).getName() + "'");
		} else {
			sender.sendMessage(ChatColor.GOLD + "Cleared all tags from entity '" + entity.getUniqueId() + "'");
		}
	}
}
