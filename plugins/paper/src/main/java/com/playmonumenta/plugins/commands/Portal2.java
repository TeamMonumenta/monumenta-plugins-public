package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.portals.PortalManager;
import org.bukkit.entity.Player;

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
