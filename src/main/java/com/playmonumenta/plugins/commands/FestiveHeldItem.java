package com.playmonumenta.plugins.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.utils.CommandUtils;

public class FestiveHeldItem extends GenericCommand {
	public static void register() {
		registerPlayerCommand("festivehelditem", "monumenta.command.festivehelditem",
		                      (sender, player) -> {
		                          run(sender, player);
		                      });
	}

	private static void run(CommandSender sender, Player player) {
		CommandUtils.enchantify(sender, player, "King's Valley", "Festive", "Decorated by");
	}
}
