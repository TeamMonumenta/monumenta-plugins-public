package com.playmonumenta.plugins.commands;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.portals.PortalManager;

public class Portal2 extends GenericCommand {
	public static void register() {

		registerPlayerCommand("portal2", "monumenta.command.portal2",
		                      (sender, player) -> {
		                          run(player);
		                      });
	}

	private static void run(Player player) {
		PortalManager.spawnPortal(player, 2);
	}
}
