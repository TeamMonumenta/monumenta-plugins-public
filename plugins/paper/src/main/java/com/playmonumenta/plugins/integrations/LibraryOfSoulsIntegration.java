package com.playmonumenta.plugins.integrations;

import java.util.Set;
import java.util.logging.Logger;

import com.playmonumenta.libraryofsouls.LibraryOfSoulsAPI;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

public class LibraryOfSoulsIntegration {
	private static LibraryOfSoulsIntegration INSTANCE = null;

	public LibraryOfSoulsIntegration(Logger logger) {
		logger.info("Enabling LibraryOfSouls integration");
	}

	public static Entity summon(Location loc, String soulName) {
		if (INSTANCE != null) {
			return LibraryOfSoulsAPI.summon(loc, soulName);
		}
		return null;
	}

	public static Set<String> getSoulNames() {
		if (INSTANCE != null) {
			return LibraryOfSoulsAPI.getSoulNames();
		}
		return null;
	}

	public static Set<String> getSoulLocations() {
		if (INSTANCE != null) {
			return LibraryOfSoulsAPI.getSoulLocations();
		}
		return null;
	}
}
