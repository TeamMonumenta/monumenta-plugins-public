package com.playmonumenta.plugins.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.utils.CommandUtils;

public class HopeifyHeldItem extends GenericCommand {
	public static void register() {
		registerPlayerCommand("hopeifyhelditem", "monumenta.command.hopeifyhelditem",
		                      (sender, player) -> {
		                          run(sender, player);
		                      });
	}

	private static void run(CommandSender sender, Player player) {
		CommandUtils.enchantify(sender, player, "King's Valley", "Hope", "Infused by");
	}
}
