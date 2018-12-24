package com.playmonumenta.plugins.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;

public class DebugInfo extends GenericCommand {
	public static void register(Plugin plugin) {
		registerPlayerCommand("debuginfo", "monumenta.command.debuginfo",
		                      (sender, player) -> {
		                          run(plugin, sender, player);
		                      });
	}

	private static void run(Plugin plugin, CommandSender sender, Player player) {
		if (plugin.mPotionManager != null) {
			sender.sendMessage(ChatColor.GREEN + "Potion info for player '" + player.getName() + "': " + ChatColor.GOLD +
			                   plugin.mPotionManager.printInfo(player));
		}
		if (plugin.mAbilityManager != null) {
			sender.sendMessage(ChatColor.GREEN + "Ability info for player '" + player.getName() + "': " + ChatColor.GOLD +
			                   plugin.mAbilityManager.printInfo(player));
		}
	}
}
