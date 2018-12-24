package com.playmonumenta.plugins.rawcommands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;

public class RefreshClass extends GenericCommand {
	public static void register(Plugin plugin) {
		registerPlayerCommand("refreshclass", "monumenta.command.refreshclass",
		                      (sender, player) -> {
		                          run(plugin, sender, player);
		                      });
	}

	private static void run(Plugin plugin, CommandSender sender, Player player) {
		if (plugin.mAbilityManager != null) {
			plugin.mAbilityManager.updatePlayerAbilities(player);
		}
		sender.sendMessage(ChatColor.GOLD + "Refreshed class for player '" + player.getName() + "'");
	}
}
