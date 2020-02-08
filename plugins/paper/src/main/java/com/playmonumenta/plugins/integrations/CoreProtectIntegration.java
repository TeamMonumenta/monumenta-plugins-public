package com.playmonumenta.plugins.integrations;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;

public class CoreProtectIntegration {
	private static CoreProtectAPI getCoreProtect() {
		Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("CoreProtect");

		// Check that CoreProtect is loaded
		if (plugin == null || !(plugin instanceof CoreProtect)) {
			return null;
		}

		// Check that the API is enabled
		CoreProtectAPI coreProtect = ((CoreProtect) plugin).getAPI();
		if (coreProtect.isEnabled() == false) {
			return null;
		}

		// Check that a compatible version of the API is loaded
		if (coreProtect.APIVersion() < 6) {
			return null;
		}

		return coreProtect;
	}

	public static void logContainerTransaction(Player player, Location location) {
		CoreProtectAPI api = getCoreProtect();
		if (api != null) {
			api.logContainerTransaction(player.getName(), location);
		}
	}
}
