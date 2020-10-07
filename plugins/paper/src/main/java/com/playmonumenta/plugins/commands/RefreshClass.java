package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.InventoryUtils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
		InventoryUtils.scheduleDelayedEquipmentCheck(plugin, player, null);
		sender.sendMessage(ChatColor.GOLD + "Refreshed class for player '" + player.getName() + "'");
	}
}
