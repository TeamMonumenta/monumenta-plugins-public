package com.playmonumenta.plugins.rawcommands;

import com.playmonumenta.plugins.managers.potion.PotionManager;
import com.playmonumenta.plugins.abilities.AbilityManager;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DebugInfo extends GenericCommand {
	public static void register(PotionManager potionManager, AbilityManager abilityManager) {
		registerPlayerCommand("debuginfo", "monumenta.command.debuginfo",
		                      (sender, player) -> {
		                          run(potionManager, abilityManager, sender, player);
		                      });
	}

	private static void run(PotionManager potionManager, AbilityManager abilityManager, CommandSender sender, Player player) {
		sender.sendMessage(ChatColor.GREEN + "Potion info for player '" + player.getName() + "': " + ChatColor.GOLD +
		                   potionManager.printInfo(player));
		sender.sendMessage(ChatColor.GREEN + "Ability info for player '" + player.getName() + "': " + ChatColor.GOLD +
		                   abilityManager.printInfo(player));
	}
}
