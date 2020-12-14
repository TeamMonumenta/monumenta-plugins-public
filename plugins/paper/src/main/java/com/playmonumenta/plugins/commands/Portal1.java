package com.playmonumenta.plugins.commands;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.portals.PortalManager;

public class Portal1 extends GenericCommand {
	public static void register() {

		registerPlayerCommand("portal1", "monumenta.command.portal1",
		                      (sender, player) -> {
		                          run(player);
		                      });
	}

	private static void run(Player player) {
		PortalManager.spawnPortal(player, 1);
	}
}
