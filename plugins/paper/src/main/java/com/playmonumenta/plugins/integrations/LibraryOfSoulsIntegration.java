package com.playmonumenta.plugins.integrations;

import java.util.Set;
import java.util.logging.Logger;

import com.playmonumenta.libraryofsouls.LibraryOfSoulsAPI;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

public class LibraryOfSoulsIntegration {
	private static boolean ENABLED = false;

	public LibraryOfSoulsIntegration(Logger logger) {
		logger.info("Enabling LibraryOfSouls integration");
		ENABLED = true;
	}

	public static Entity summon(Location loc, String soulName) {
		if (ENABLED) {
			return LibraryOfSoulsAPI.summon(loc, soulName);
		}
		return null;
	}

	public static Set<String> getSoulNames() {
		if (ENABLED) {
			return LibraryOfSoulsAPI.getSoulNames();
		}
		return null;
	}

	public static Set<String> getSoulLocations() {
		if (ENABLED) {
			return LibraryOfSoulsAPI.getSoulLocations();
		}
		return null;
	}
}
