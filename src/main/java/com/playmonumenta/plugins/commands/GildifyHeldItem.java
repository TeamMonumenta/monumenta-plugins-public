package com.playmonumenta.plugins.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.utils.CommandUtils;

public class GildifyHeldItem extends GenericCommand {
	public static void register() {
		registerPlayerCommand("gildifyhelditem", "monumenta.command.gildifyhelditem",
		                      (sender, player) -> {
		                          run(sender, player);
		                      });
	}

	private static void run(CommandSender sender, Player player) {
		CommandUtils.enchantify(sender, player, "King's Valley", "Gilded", "Gilded by", true);
	}
}
