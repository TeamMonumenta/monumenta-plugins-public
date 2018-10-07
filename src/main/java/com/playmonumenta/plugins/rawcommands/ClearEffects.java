package com.playmonumenta.plugins.rawcommands;

import com.playmonumenta.plugins.managers.potion.PotionManager;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClearEffects extends GenericCommand {
	public static void register(PotionManager potionManager) {
		registerPlayerCommand("cleareffects", "monumenta.command.cleareffects",
		                      (sender, player) -> {
		                          run(potionManager, sender, player);
		                      });
	}

	private static void run(PotionManager potionManager, CommandSender sender, Player player) {
		potionManager.clearAllPotions(player);

		sender.sendMessage(ChatColor.GOLD + "Cleared potion effects for player '" + player.getName() + "'");
	}
}
