package com.playmonumenta.plugins.rawcommands;

import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.managers.potion.PotionManager;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RefreshClass extends GenericCommand {
	public static void register(PotionManager potionManager, AbilityManager abilityManager) {
		registerPlayerCommand("refreshclass", "monumenta.command.refreshclass",
		                      (sender, player) -> {
		                          run(potionManager, abilityManager, sender, player);
		                      });
	}

	private static void run(PotionManager potionManager, AbilityManager abilityManager, CommandSender sender, Player player) {
		abilityManager.updatePlayerAbilities(player);
		potionManager.refreshClassEffects(player);
		sender.sendMessage(ChatColor.GOLD + "Refreshed class for player '" + player.getName() + "'");
	}
}
