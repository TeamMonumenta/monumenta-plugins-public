package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.portals.PortalManager;

public class ClearPortals extends GenericCommand {
	public static void register() {

		registerPlayerCommand("clearportals", "monumenta.command.clearportals",
		                      (sender, player) -> {
		                          PortalManager.clearAllPortals(player);
		                      });
	}
}
