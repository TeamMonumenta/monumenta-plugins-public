package com.playmonumenta.plugins.commands;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.portals.PortalManager;

public class ClearPortals extends GenericCommand {
	public static void register() {

		registerPlayerCommand("clearportals", "monumenta.command.clearportals",
		                      (sender, player) -> {
		                          run(player);
		                      });
	}

	private static void run(Player player) {
		PortalManager.clearPortal(player, 1);
		PortalManager.clearPortal(player, 2);
	}
}
