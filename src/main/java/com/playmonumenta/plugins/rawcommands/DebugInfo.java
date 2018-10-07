package com.playmonumenta.plugins.rawcommands;

import com.playmonumenta.plugins.managers.potion.PotionManager;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DebugInfo extends GenericCommand {
	public static void register(PotionManager potionManager) {
		registerPlayerCommand("debuginfo", "monumenta.command.debuginfo",
		                      (sender, player) -> {
		                          run(potionManager, sender, player);
		                      });
	}

	private static void run(PotionManager potionManager, CommandSender sender, Player player) {
		sender.sendMessage(ChatColor.GOLD + "Potion info for player '" + player.getName() + "': " +
		                   potionManager.printInfo(player));
	}
}
