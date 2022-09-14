package com.playmonumenta.plugins.integrations;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

// TODO Make an actual API to hook this into, rather than running commands
public class MonumentaNetworkChatIntegration {
	public static void refreshPlayer(Player player) {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
		                       "execute as " + player.getName() + " run chat player refresh");
	}

	public static void createGuildChannel(String guildTag, String cleanGuildName) {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "chat new " + guildTag + " global true group." + cleanGuildName);
	}
}
