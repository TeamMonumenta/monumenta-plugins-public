package com.playmonumenta.plugins.integrations;

import java.util.Set;

import com.playmonumenta.libraryofsouls.LibraryOfSoulsAPI;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

class LibraryOfSoulsIntegrationConnector {
	protected Entity summon(Location loc, String soulName) {
		return LibraryOfSoulsAPI.summon(loc, soulName);
	}

	protected Set<String> getSoulNames() {
		return LibraryOfSoulsAPI.getSoulNames();
	}

	protected Set<String> getSoulLocations() {
		return LibraryOfSoulsAPI.getSoulLocations();
	}
}
