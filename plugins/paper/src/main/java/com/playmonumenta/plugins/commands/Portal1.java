package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.portals.PortalManager;
import org.bukkit.entity.Player;

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
