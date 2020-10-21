package com.playmonumenta.plugins.integrations;

import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public class LibraryOfSoulsIntegration {
	private static LibraryOfSoulsIntegrationConnector CONNECTOR = null;
	private static boolean LOS_CHECKED = false;

	private static void ensureInitialized() {
		if (LOS_CHECKED) {
			return;
		}

		LOS_CHECKED = true;

		Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("LibraryOfSouls");
		if (plugin != null) {
			CONNECTOR = new LibraryOfSoulsIntegrationConnector();
		}
	}

	public static Entity summon(Location loc, String soulName) {
		ensureInitialized();

		if (CONNECTOR == null) {
			return null;
		}
		return CONNECTOR.summon(loc, soulName);
	}

	public static Set<String> getSoulNames() {
		ensureInitialized();

		if (CONNECTOR == null) {
			return null;
		}
		return CONNECTOR.getSoulNames();
	}

	public static Set<String> getSoulLocations() {
		ensureInitialized();

		if (CONNECTOR == null) {
			return null;
		}
		return CONNECTOR.getSoulNames();
	}
}
