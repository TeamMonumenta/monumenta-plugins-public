package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.InventoryUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
			plugin.mAbilityManager.updatePlayerAbilities(player, true);
		}
		InventoryUtils.scheduleDelayedEquipmentCheck(plugin, player, null);
		sender.sendMessage(Component.text("Refreshed class for player '" + player.getName() + "'", NamedTextColor.GOLD));
	}
}
