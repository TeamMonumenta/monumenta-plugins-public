package com.playmonumenta.plugins.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class RemoveTags extends GenericCommand {
	public static void register() {
		registerEntityCommand("removetags", "monumenta.command.removetags", RemoveTags::run);
	}

	private static void run(CommandSender sender, Entity entity) {
		entity.getScoreboardTags().clear();

		if (entity instanceof Player player) {
			sender.sendMessage(Component.text("Cleared all tags from player '" + player.getName() + "'", NamedTextColor.GOLD));
		} else {
			sender.sendMessage(Component.text("Cleared all tags from entity '" + entity.getUniqueId() + "'", NamedTextColor.GOLD));
		}
	}
}
